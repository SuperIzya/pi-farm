export const RouteNames = {
  base: '/ui',
  inventory: 'inventory',
  controller: 'controller',
  periphery: 'periphery',
  new: 'new',
  edit: 'edit/:id'
}

export const composeRoutes = (...routes: string[]): string => routes.join('/')
