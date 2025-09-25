import React from 'react';
import ComponentCreator from '@docusaurus/ComponentCreator';

export default [
  {
    path: '/sprout/ko/',
    component: ComponentCreator('/sprout/ko/', '281'),
    routes: [
      {
        path: '/sprout/ko/',
        component: ComponentCreator('/sprout/ko/', '8ed'),
        routes: [
          {
            path: '/sprout/ko/',
            component: ComponentCreator('/sprout/ko/', '07c'),
            routes: [
              {
                path: '/sprout/ko/architecture/ioc-container',
                component: ComponentCreator('/sprout/ko/architecture/ioc-container', '10d'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/overview/configuration',
                component: ComponentCreator('/sprout/ko/overview/configuration', 'd5c'),
                exact: true,
                sidebar: "tutorialSidebar"
              },
              {
                path: '/sprout/ko/overview/intro',
                component: ComponentCreator('/sprout/ko/overview/intro', 'a39'),
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
                component: ComponentCreator('/sprout/ko/overview/roadmap', 'a21'),
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
