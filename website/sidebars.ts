import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  tutorialSidebar: [
    {
      type: 'category',
      label: 'Overview',
      items: [
        'overview/intro',
        'overview/quickstart',
        'overview/configuration',
        'overview/roadmap',
      ],
    },
    {
      type: 'category',
      label: 'Architecture',
      items: [
        'architecture/ioc-container',
        'architecture/http-server',
        'architecture/websocket',
        'architecture/mvc-controller-mapping',
        'architecture/mvc-argument-resolution',
        'architecture/http-message-parsing',
        'architecture/request-dispatching',
        'architecture/aop-advice-advisor',
        'architecture/aop-di-integration',
      ],
    },
    {
      type: 'category',
      label: 'Guides',
      items: [
        'guides/mvc-controller-mapping-guide',
      ],
    },
  ],
};

export default sidebars;