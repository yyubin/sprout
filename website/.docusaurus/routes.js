import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/sprout/ko/',
    component: ComponentCreator('/sprout/ko/', '827'),
    routes: [
      {
        path: '/sprout/ko/',
        component: ComponentCreator('/sprout/ko/', '867'),
        routes: [
          {
            path: '/sprout/ko/',
            component: ComponentCreator('/sprout/ko/', '7a7'),
            routes: [
              {
                path: '/sprout/ko/architecture/http-message-parsing',
                component: ComponentCreator('/sprout/ko/architecture/http-message-parsing', 'a60'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/architecture/ioc-container',
                component: ComponentCreator('/sprout/ko/architecture/ioc-container', '10d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/architecture/mvc-argument-resolution',
                component: ComponentCreator('/sprout/ko/architecture/mvc-argument-resolution', '2f6'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/architecture/mvc-controller-mapping',
                component: ComponentCreator('/sprout/ko/architecture/mvc-controller-mapping', '2e0'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/guides/mvc-controller-mapping-guide',
                component: ComponentCreator('/sprout/ko/guides/mvc-controller-mapping-guide', 'c79'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/overview/configuration',
                component: ComponentCreator('/sprout/ko/overview/configuration', '7f2'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/overview/intro',
                component: ComponentCreator('/sprout/ko/overview/intro', 'e15'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/overview/quickstart',
                component: ComponentCreator('/sprout/ko/overview/quickstart', 'b1e'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/overview/roadmap',
                component: ComponentCreator('/sprout/ko/overview/roadmap', 'c11'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/',
                component: ComponentCreator('/sprout/ko/', '4a5'),
                exact: true
              }
            ]
          }
        ]
      }
    ]
  },
  {
    path: '*',
    component: ComponentCreator('*'),
  },
];
