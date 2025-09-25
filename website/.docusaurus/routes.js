import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/sprout/',
    component: ComponentCreator('/sprout/', 'f4a'),
    routes: [
      {
        path: '/sprout/',
        component: ComponentCreator('/sprout/', 'cc6'),
        routes: [
          {
            path: '/sprout/',
            component: ComponentCreator('/sprout/', 'ef5'),
            routes: [
              {
                path: '/sprout/architecture/ioc-container',
                component: ComponentCreator('/sprout/architecture/ioc-container', 'b9b'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/overview/configuration',
                component: ComponentCreator('/sprout/overview/configuration', 'de3'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/overview/intro',
                component: ComponentCreator('/sprout/overview/intro', '54c'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/overview/quickstart',
                component: ComponentCreator('/sprout/overview/quickstart', 'f79'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/overview/roadmap',
                component: ComponentCreator('/sprout/overview/roadmap', '963'),
                exact: true,
                sidebar: "tutorialSidebar"
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
