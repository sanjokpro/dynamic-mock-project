# Dynamic Mock API Server - Frontend

Angular 17+ frontend application for managing dynamic mock API routes.

## Features

- **Route Management**: Create, edit, delete, and activate/deactivate mock routes
- **Monaco Editor**: Code editor with syntax highlighting for scripts and JSON templates
- **Material Design**: Beautiful UI with custom Nepali rupee papernote theme
- **Real-time Updates**: View route status and details in real-time

## Prerequisites

- Node.js 18+ and npm
- Angular CLI 17+

## Setup

1. Install dependencies:
```bash
cd frontend
npm install
```

2. Start the development server:
```bash
npm start
```

The application will be available at `http://localhost:4200`

## Build

To build for production:

```bash
npm run build
```

The build artifacts will be stored in the `dist/` directory.

## Configuration

The API base URL is configured in `src/app/services/route.service.ts`. By default, it points to `http://localhost:8080/api/routes`.

To change the API URL, update the `apiUrl` property in the `RouteService` class.

## Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── models/          # Data models/interfaces
│   │   ├── services/        # API services
│   │   ├── routes/          # Route management components
│   │   │   ├── route-list/      # Route list component
│   │   │   ├── route-form/      # Create/edit route form
│   │   │   └── route-detail/    # Route detail view
│   │   ├── shared/          # Shared components
│   │   │   └── monaco-editor/   # Monaco Editor wrapper
│   │   ├── app.component.ts     # Main app component
│   │   └── app.routes.ts        # Routing configuration
│   ├── styles.scss          # Global styles and theme
│   └── index.html           # Main HTML file
├── angular.json             # Angular CLI configuration
├── package.json             # Dependencies
└── tsconfig.json            # TypeScript configuration
```

## Development

### Running unit tests

```bash
npm test
```

### Code scaffolding

Run `ng generate component component-name` to generate a new component.

## Monaco Editor

The application uses Monaco Editor (the same editor that powers VS Code) for editing scripts and JSON templates. The editor is loaded from CDN and supports:

- JavaScript syntax highlighting
- Python syntax highlighting
- JSON syntax highlighting
- Code completion
- Error detection

## Theme

The application uses a custom Angular Material theme inspired by Nepali rupee papernote colors, featuring warm, earthy tones that create a professional and elegant appearance.
