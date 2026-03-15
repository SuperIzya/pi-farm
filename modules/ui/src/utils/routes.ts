export const RouteNames = {
  base: '/',
  inventory: 'inventory',
  controller: 'controller',
  periphery: 'periphery',
  configuration: 'configuration',
  new: 'new',
  edit: 'edit/:id'
}

export const composeRoutes = (...routes: string[]): string =>
  routes[0] === RouteNames.base ? `/${routes.slice(1).join('/')}` : routes.join('/')
