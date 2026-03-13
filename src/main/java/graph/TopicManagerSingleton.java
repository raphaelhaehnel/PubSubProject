package graph;

public class TopicManagerSingleton {

    public static TopicManager get() {
        return TopicManager.instance;
    }

    static class TopicManager {

        private static final TopicManager instance = new TopicManager();

        private TopicManager() {

        }


    }
}
