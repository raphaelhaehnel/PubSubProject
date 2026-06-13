import React, { useState, useEffect, useRef } from 'react';
import { Network } from 'vis-network';
import { DataSet } from 'vis-data';

// API SERVICE
const ApiService = {
  getGraph: async () => {
    const res = await fetch('/graph');
    return res.json();
  },
  deployConfig: async (text) => {
    const params = new URLSearchParams();
    params.append("config", text);
    const res = await fetch('/upload', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params
    });
    return res.json();
  },
  publishMessage: async (topic, message) => {
    const res = await fetch(`/publish?topic=${encodeURIComponent(topic)}&message=${encodeURIComponent(message)}`);
    return res.json();
  },
  resetGraph: async () => {
    const res = await fetch('/reset', { method: 'POST' });
    return res.json();
  }
};


// SUB COMPONENTS

// Component handling user actions (Deploy, Publish, Reset)
const ControlPanel = ({ onDeploy, onPublish, onReset }) => {
  const [topic, setTopic] = useState('');
  const [message, setMessage] = useState('');

  // New state variables for the advanced deployment panel
  const [selectedFile, setSelectedFile] = useState(null);
  const [jsonSnippet, setJsonSnippet] = useState('');
  const [mode, setMode] = useState('file'); // 'file' | 'snippet'
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
    setIsDeployed(false); // Enable the deploy button for the new file
  };

  const handleSnippetChange = (e) => {
    setJsonSnippet(e.target.value);
    setIsDeployed(false); // Enable the deploy button for the new edit
  };

  const handleDeployClick = async () => {
    if (mode === 'file' && selectedFile) {
      const text = await selectedFile.text();
      onDeploy(text);
      setIsDeployed(true);
    } else if (mode === 'snippet' && jsonSnippet.trim()) {
      onDeploy(jsonSnippet);
      setIsDeployed(true);
    }
  };

  const handleClearGraph = () => {
    // An empty agents array replaces the server configuration with a blank graph
    onDeploy('{"agents": []}');
    setSelectedFile(null);
    setJsonSnippet('');
    setIsDeployed(true);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (topic && message) {
      onPublish(topic, message);
      setTopic('');
      setMessage('');
    }
  };

  // Compute whether the deploy button should be active
  const canDeploy = (mode === 'file' && selectedFile && !isDeployed) ||
                    (mode === 'snippet' && jsonSnippet.trim() && !isDeployed);

  return (
    <div style={styles.sidebarLeft}>
      <h2 style={styles.heading}>Deploy Config</h2>
      
      <div style={styles.tabContainer}>
        <div onClick={() => setMode('file')} style={mode === 'file' ? styles.activeTab : styles.tab}>File</div>
        <div onClick={() => setMode('snippet')} style={mode === 'snippet' ? styles.activeTab : styles.tab}>Snippet</div>
      </div>

      {mode === 'file' ? (
        <div
          onDrop={handleDrop}
          onDragOver={(e) => e.preventDefault()}
          onClick={() => document.getElementById('configFile').click()}
          style={{ ...styles.dropZone, marginBottom: '10px' }}
        >
          Drop your .json config here
          <br /><small style={styles.textMuted}>or click to select</small>
          <input
            type="file"
            id="configFile"
            accept=".json"
            style={{ display: 'none' }}
            onChange={(e) => { 
              if (e.target.files.length) handleFileSelect(e.target.files[0]); 
              e.target.value = null; // Reset value so re-selecting the same file triggers onChange
            }}
          />
          {selectedFile && <div style={styles.fileName}>Selected: {selectedFile.name}</div>}
        </div>
      ) : (
        <textarea
          style={styles.textArea}
          placeholder='{ "agents": [ ... ] }'
          value={jsonSnippet}
          onChange={handleSnippetChange}
        />
      )}

      <button 
        onClick={handleDeployClick} 
        disabled={!canDeploy} 
        style={canDeploy ? styles.buttonSuccess : styles.buttonDisabled}
      >
        Deploy Graph
      </button>
      
      <button onClick={handleClearGraph} style={styles.buttonWarning}>
        Clear Graph
      </button>

      <h2 style={styles.heading}>Publish Message</h2>
      <form onSubmit={handleSubmit} style={styles.form}>
        <input placeholder="Topic Name" value={topic} onChange={(e) => setTopic(e.target.value)} style={styles.input} required />
        <input placeholder="Message Body" value={message} onChange={(e) => setMessage(e.target.value)} style={styles.input} required />
        <button type="submit" style={styles.buttonPrimary}>Send Message</button>
      </form>

      <div style={styles.spacer}>
        <button onClick={onReset} style={styles.buttonDanger}>
          Reset all topics to 0
        </button>
      </div>
    </div>
  );
};

// Component handling display of available topics
const TopicList = ({ topics }) => (
  <div style={styles.sidebarRight}>
    <h2 style={styles.heading}>Topics</h2>
    {topics.length === 0 ? (
      <p style={styles.textMuted}>No topics yet.</p>
    ) : (
      <div style={styles.topicList}>
        {topics.map((t) => (
          <div key={t.name} style={styles.topicItem}>
            <strong style={styles.topicName}>{t.name}</strong>
            <span style={styles.topicBadge}>{t.value}</span>
          </div>
        ))}
      </div>
    )}
  </div>
);

// MAIN APP COMPONENT
export default function App() {
  const [topics, setTopics] = useState([]);

  const containerRef = useRef(null);
  const networkRef = useRef(null);
  const nodesRef = useRef(new DataSet([]));
  const edgesRef = useRef(new DataSet([]));

  useEffect(() => {
    if (containerRef.current && !networkRef.current) {
      networkRef.current = new Network(
        containerRef.current,
        { nodes: nodesRef.current, edges: edgesRef.current },
        { 
          nodes: { shape: "dot", size: 16 }, 
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
      nodesRef.current.add(data.nodes);
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
      console.error("Failed to load graph", err);
    }
  };

  const handleDeploy = async (configText) => {
    try {
      const data = await ApiService.deployConfig(configText);
      updateGraph(data);
    } catch (err) {
      console.error("Failed to deploy config", err);
    }
  };

  const handlePublish = async (topic, msg) => {
    try {
      const data = await ApiService.publishMessage(topic, msg);
      if (data.topics) setTopics(data.topics);
      loadGraphData(true); // Trust the server's topics, skip extraction!
    } catch (err) {
      console.error("Failed to publish", err);
    }
  };

  const handleReset = async () => {
    try {
      const data = await ApiService.resetGraph();
      if (data.topics) setTopics(data.topics);
      loadGraphData(true); // Trust the server's topics, skip extraction!
    } catch (err) {
      console.error("Failed to reset", err);
    }
  };

  return (
    <div style={styles.container}>
      <ControlPanel onDeploy={handleDeploy} onPublish={handlePublish} onReset={handleReset} />
      
      <div style={styles.graphContainer}>
        <div ref={containerRef} style={styles.graph} />
      </div>

      <TopicList topics={topics} />
    </div>
  );
}

// STYLES OOBJECT
// Centralizing CSS keeps JSX readable and strictly separates presentation logic.
const styles = {
  container: { display: 'flex', height: '100vh', margin: 0, fontFamily: 'sans-serif', color: '#333' },
  sidebarLeft: { width: '300px', padding: '20px', background: '#f8f9fa', borderRight: '1px solid #dee2e6', display: 'flex', flexDirection: 'column' },
  sidebarRight: { width: '250px', padding: '20px', background: '#f8f9fa', borderLeft: '1px solid #dee2e6', overflowY: 'auto' },
  graphContainer: { flex: 1, position: 'relative', background: '#fff' },
  graph: { width: '100%', height: '100%' },
  heading: { marginTop: 0 },
  dropZone: { border: '2px dashed #adb5bd', borderRadius: '8px', padding: '30px 10px', textAlign: 'center', cursor: 'pointer', background: '#fff', transition: 'border 0.2s' },
  textMuted: { color: '#6c757d' },
  form: { display: 'flex', flexDirection: 'column', gap: '10px' },
  input: { padding: '8px', borderRadius: '4px', border: '1px solid #ced4da' },
  buttonPrimary: { padding: '10px', background: '#0d6efd', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' },
  buttonDanger: { width: '100%', padding: '12px', background: '#dc3545', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' },
  spacer: { marginTop: 'auto', paddingTop: '20px' },
  topicList: { display: 'flex', flexDirection: 'column', gap: '10px' },
  topicItem: { background: '#fff', padding: '10px', borderRadius: '6px', border: '1px solid #dee2e6', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  topicName: { color: '#0d6efd' },
  topicBadge: { background: '#e9ecef', padding: '2px 8px', borderRadius: '12px', fontSize: '0.9em' },
  tabContainer: { display: 'flex', marginBottom: '15px', gap: '5px' },
  tab: { flex: 1, padding: '8px', cursor: 'pointer', border: '1px solid #ced4da', background: '#e9ecef', borderRadius: '4px', textAlign: 'center', fontSize: '0.9em' },
  activeTab: { flex: 1, padding: '8px', cursor: 'pointer', border: '1px solid #0d6efd', background: '#0d6efd', color: '#fff', borderRadius: '4px', textAlign: 'center', fontWeight: 'bold', fontSize: '0.9em' },
  textArea: { width: '100%', height: '120px', padding: '8px', borderRadius: '4px', border: '1px solid #ced4da', resize: 'vertical', boxSizing: 'border-box', fontFamily: 'monospace', fontSize: '0.85em' },
  fileName: { marginTop: '10px', fontSize: '0.9em', color: '#0d6efd', wordBreak: 'break-all', fontWeight: 'bold' },
  buttonSuccess: { width: '100%', padding: '10px', background: '#198754', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', marginBottom: '10px' },
  buttonDisabled: { width: '100%', padding: '10px', background: '#ced4da', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'not-allowed', fontWeight: 'bold', marginBottom: '10px' },
  buttonWarning: { width: '100%', padding: '10px', background: '#ffc107', color: '#000', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', marginBottom: '30px' }
};