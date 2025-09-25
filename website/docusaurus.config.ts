import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
  title: 'Sprout Framework',
  tagline: 'A lightweight Java web framework built from scratch',
  favicon: 'img/favicon.ico',

  url: 'https://yyubin.github.io',
  baseUrl: '/sprout/',

  organizationName: 'yyubin',
  projectName: 'sprout',

  onBrokenLinks: 'warn',
  onBrokenMarkdownLinks: 'warn',

  i18n: {
    defaultLocale: 'en',
    locales: ['en', 'ko'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          routeBasePath: '/',
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/yyubin/sprout/tree/main/website/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    image: 'img/sprout-social-card.jpg',
    navbar: {
      title: 'Sprout',
      logo: {
        alt: 'Sprout Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'tutorialSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          to: '/tests/',
          label: 'Tests',
          position: 'left',
        },
        {
          to: '/coverage/',
          label: 'Coverage',
          position: 'left',
        },
        {
          href: 'https://github.com/yyubin/sprout',
          label: 'GitHub',
          position: 'right',
        },
        {
          type: 'localeDropdown',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Documentation',
          items: [
            {
              label: 'Getting Started',
              to: '/overview/quickstart',
            },
            {
              label: 'Architecture',
              to: '/architecture/ioc-container',
            },
            {
              label: 'Guides',
              to: '/guides/getting-started',
            },
          ],
        },
        {
          title: 'Quality',
          items: [
            {
              label: 'Test Reports',
              to: '/tests/',
            },
            {
              label: 'Coverage Reports',
              to: '/coverage/',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/yyubin/sprout',
            },
            {
              label: 'Issues',
              href: 'https://github.com/yyubin/sprout/issues',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Sprout Framework. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'yaml', 'bash'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;