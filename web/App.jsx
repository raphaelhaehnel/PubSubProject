import React, { useState, useEffect, useRef } from 'react';
import { ConfigDTO } from './dtos/ConfigDTO.js';
import { Network } from 'vis-network';
import { DataSet } from 'vis-data';

// API SERVICE
const handleApiError = async (res) => {
  if (!res.ok) {
    let errorText = "An unexpected server error occurred.";
    try {
      const text = await res.text();
      try {
        const errJson = JSON.parse(text);
        errorText = errJson.message || errJson.error || text;
      } catch {
        errorText = text || errorText;
      }
    } catch (e) {
      // Safe fallback
    }
    throw new Error(errorText);
  }
  return res.json();
};

const ApiService = {
  getGraph: async () => {
    const res = await fetch('/graph');
    return handleApiError(res);
  },
  deployConfig: async (text) => {
    const params = new URLSearchParams();
    params.append("config", text);
    const res = await fetch('/upload', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params
    });
    return handleApiError(res);
  },
  publishMessage: async (topic, message) => {
    const res = await fetch(`/publish?topic=${encodeURIComponent(topic)}&message=${encodeURIComponent(message)}`);
    return handleApiError(res);
  },
  resetGraph: async () => {
    const res = await fetch('/reset', { method: 'POST' });
    return handleApiError(res);
  },
  clearGraph: async () => {
    const res = await fetch('/clear', { method: 'POST' });
    return handleApiError(res);
  }
};

// SUB COMPONENTS
const ControlPanel = ({ onDeploy, onPublish, onReset, onClear, onError }) => {
  const [topic, setTopic] = useState('');
  const [message, setMessage] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [jsonSnippet, setJsonSnippet] = useState('');
  const [mode, setMode] = useState('file'); 
  const [isDeployed, setIsDeployed] = useState(true);

  const handleDrop = (e) => {
    e.preventDefault();
    if (e.dataTransfer.files?.length) {
      handleFileSelect(e.dataTransfer.files[0]);
    }
  };

  const handleFileSelect = (file) => {
    setSelectedFile(file);
    setMode('file');
    setIsDeployed(false);
  };

  const handleSnippetChange = (e) => {
    setJsonSnippet(e.target.value);
    setIsDeployed(false);
  };

  const handleDeployClick = async () => {
    try {
      let textToDeploy = '';
      let fileName = null;

      if (mode === 'file' && selectedFile) {
        fileName = selectedFile.name;
        textToDeploy = await selectedFile.text();
      } else if (mode === 'snippet' && jsonSnippet.trim()) {
        textToDeploy = jsonSnippet;
      }

      if (fileName && !fileName.toLowerCase().endsWith('.json')) {
        throw new Error("Invalid file type. Please upload a .json file.");
      }

      const configDto = new ConfigDTO(textToDeploy);

      await onDeploy(textToDeploy);
      setIsDeployed(true);
      
    } catch (err) {
      onError(err.message);
    }
  };

  const handleClearGraph = async () => {
    try {
      await onClear();
      setSelectedFile(null);
      setJsonSnippet('');
      setIsDeployed(true);
    } catch(err) {
      onError(err.message);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (topic && message) {
      onPublish(topic, message);
      setTopic('');
      setMessage('');
    }
  };

  const canDeploy = (mode === 'file' && selectedFile && !isDeployed) ||
                    (mode === 'snippet' && jsonSnippet.trim() && !isDeployed);

  return (
    <div className="sidebar-left">
      <h2 className="heading">Deploy Config</h2>
      
      <div className="tab-container">
        <div onClick={() => setMode('file')} className={mode === 'file' ? "active-tab" : "tab"}>File</div>
        <div onClick={() => setMode('snippet')} className={mode === 'snippet' ? "active-tab" : "tab"}>Snippet</div>
      </div>

      {mode === 'file' ? (
        <div
          onDrop={handleDrop}
          onDragOver={(e) => e.preventDefault()}
          onClick={() => document.getElementById('configFile').click()}
          className="drop-zone"
        >
          Drop your .json config here
          <br /><small className="text-muted">or click to select</small>
          <input
            type="file"
            id="configFile"
            accept=".json"
            style={{ display: 'none' }}
            onChange={(e) => { 
              if (e.target.files.length) handleFileSelect(e.target.files[0]); 
              e.target.value = null;
            }}
          />
          {selectedFile && <div className="file-name">Selected: {selectedFile.name}</div>}
        </div>
      ) : (
        <textarea
          className="text-area"
          placeholder='{ "agents": [ ... ] }'
          value={jsonSnippet}
          onChange={handleSnippetChange}
        />
      )}

      <button onClick={handleDeployClick} disabled={!canDeploy} className={canDeploy ? "button-success" : "button-disabled"}>
        Deploy Graph
      </button>
      
      <button onClick={handleClearGraph} className="button-warning">
        Clear Graph
      </button>

      <h2 className="heading">Publish Message</h2>
      <form onSubmit={handleSubmit} className="form">
        <input placeholder="Topic Name" value={topic} onChange={(e) => setTopic(e.target.value)} className="input" required />
        <input placeholder="Message Body" value={message} onChange={(e) => setMessage(e.target.value)} className="input" required />
        <button type="submit" className="button-primary">Send Message</button>
      </form>

      <div className="spacer">
        <button onClick={onReset} className="button-danger">
          Reset all topics to 0
        </button>
      </div>
    </div>
  );
};

const TopicList = ({ topics }) => (
  <div className="sidebar-right">
    <h2 className="heading">Topics</h2>
    {topics.length === 0 ? (
      <p className="text-muted">No topics yet.</p>
    ) : (
      <div className="topic-list">
        {topics.map((t) => (
          <div key={t.name} className="topic-item">
            <strong className="topic-name">{t.name}</strong>
            <span className="topic-badge">{t.value}</span>
          </div>
        ))}
      </div>
    )}
  </div>
);

// MAIN APP COMPONENT
export default function App() {
  const [topics, setTopics] = useState([]);
  const [toast, setToast] = useState(null);

  const containerRef = useRef(null);
  const networkRef = useRef(null);
  const nodesRef = useRef(new DataSet([]));
  const edgesRef = useRef(new DataSet([]));

  const triggerError = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(null), 10000); 
  };

  useEffect(() => {
    if (containerRef.current && !networkRef.current) {
      networkRef.current = new Network(
        containerRef.current,
        { nodes: nodesRef.current, edges: edgesRef.current },
        { 
          edges: { arrows: "to" }, 
          physics: { 
            solver: 'repulsion',
            repulsion: { nodeDistance: 150, springLength: 150 }
          } 
        }
      );
      loadGraphData();
    }

    return () => {
      if (networkRef.current) {
        networkRef.current.destroy();
        networkRef.current = null;
      }
    };
  }, []);

  const updateGraph = (data, skipTopicUpdate = false) => {
    if (data?.nodes && data?.edges) {
      nodesRef.current.clear();
      edgesRef.current.clear();

      const styledNodes = data.nodes.map(n => {
        const isAgent = n.label && n.label.includes('Agent');
        return {
          ...n,
          shape: isAgent ? 'dot' : 'box',
          size: isAgent ? 16 : undefined,
          shapeProperties: isAgent ? {} : { borderRadius: 0 } 
        };
      });

      nodesRef.current.add(styledNodes);
      edgesRef.current.add(data.edges);

      if (!skipTopicUpdate) {
        const parsedTopics = data.nodes
          .filter(n => {
            const label = String(n.label || '');
            return label && !label.includes('Agent');
          })
          .map(n => {
            const rawLabel = String(n.label || '');
            const parts = rawLabel.replace(/\\n/g, '\n').split('\n');
            let name = parts[0].trim();
            if ((name.length === 2 && name.startsWith('T')) || (name.startsWith('T') && n.color === 'lightblue')) {
              name = name.substring(1);
            }
            let value = parts.length > 1 ? parts[1].trim() : 'null';
            if (value.startsWith('(') && value.endsWith(')')) {
              value = value.slice(1, -1);
            }
            return { name, value };
          })
          .sort((a, b) => a.name.localeCompare(b.name));

        setTopics(parsedTopics);
      }
    }
  };

  const loadGraphData = async (skipTopicUpdate = false) => {
    try {
      const data = await ApiService.getGraph();
      updateGraph(data, skipTopicUpdate);
    } catch (err) {
      triggerError("Failed to load graph connection.");
    }
  };

  const handleDeploy = async (configText) => {
    try {
      const data = await ApiService.deployConfig(configText);
      updateGraph(data);
    } catch (err) {
      triggerError(err.message);
      throw err;
    }
  };

  const handleClear = async () => {
    try {
      const data = await ApiService.clearGraph();
      updateGraph(data);
    } catch (err) {
      triggerError(err.message);
      throw err;
    }
  };

  const handlePublish = async (topic, msg) => {
    try {
      const data = await ApiService.publishMessage(topic, msg);
      if (data.topics) setTopics(data.topics);
      loadGraphData(true);
    } catch (err) {
      triggerError(err.message);
    }
  };

  const handleReset = async () => {
    try {
      const data = await ApiService.resetGraph();
      if (data.topics) setTopics(data.topics);
      loadGraphData(true); 
    } catch (err) {
      triggerError(err.message);
    }
  };

  return (
    <div className="container">
      <ControlPanel onDeploy={handleDeploy} onPublish={handlePublish} onReset={handleReset} onClear={handleClear} onError={triggerError} />
      
      <div className="graph-container">
        <div ref={containerRef} className="graph" />
        
        {/* DISPOSABLE ERROR TOAST */}
        {toast && (
          <div className="error-toast">
            <strong style={{ display: 'block', marginBottom: '5px' }}>⚠️ Error</strong>
            {toast}
            <div className="toast-progress-bar"></div>
          </div>
        )}
      </div>

      <TopicList topics={topics} />
    </div>
  );
}