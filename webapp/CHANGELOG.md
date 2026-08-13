# Changelog

## [Unreleased] - Vue 3 Migration

### Major Changes

This release includes a complete migration from Vue 2 to Vue 3, along with Vuetify 2 to Vuetify 3 upgrade.

### Dependencies Updated

- `vue`: 2.7.x → 3.5.x
- `vue-router`: 3.x → 4.5.x
- `vuetify`: 2.5.x → 3.7.x
- `@vitejs/plugin-vue2` → `@vitejs/plugin-vue`
- `@mdi/js`: 6.x → 3.x
- `eslint-plugin-vue`: 8.x → 9.x
- `sass`: 1.32.x → 1.70.x
- `unplugin-vue-components`: 0.25.x → 0.27.x
- Added `dayjs` for date formatting (replacing `vue-moment`)
- Removed `vue-moment` (no longer needed with Vue 3)
- Removed `vue-sse` (using native SSE implementation)

### Breaking Changes

#### Vue 3 Migration

1. **Application Entry Point** (`src/main.js`)
   - Changed from `new Vue()` to `createApp()`
   - Plugins registered using `app.use()` instead of `Vue.use()`
   - Directives registered using `app.directive()`

2. **Router** (`src/router/index.js`)
   - Changed from `new VueRouter()` to `createRouter()` with `createWebHistory()`
   - Removed `Vue.use(VueRouter)`

3. **Store** (`src/plugins/store.js`)
   - Changed from Vue 2 reactivity hack to Vue 3 `reactive()` API
   - Removed `Vue.set()` calls (Vue 3 handles reactivity automatically)

4. **Plugins**
   - All plugins updated to Vue 3 plugin format with `install(app)` function
   - Global properties accessed via `app.config.globalProperties`

5. **Lifecycle Hooks**
   - `beforeDestroy` → `beforeUnmount`
   - `mounted` remains the same

6. **V-Model Changes**
   - Custom components: `v-model` now uses `modelValue` prop and `update:modelValue` event
   - Changed `@input` events to `@update:modelValue`

7. **Vue Directives**
   - Custom directive syntax updated to Vue 3 format

#### Vuetify 3 Migration

1. **Component Changes**
   - `dense` → `density="compact"`
   - `hide-details` → `variant="plain"`
   - `elevation="0"` → `variant="plain"`
   - `single-line` → kept for text fields
   - `v-btn text` → `v-btn variant="text"`
   - `v-icon small` → `v-icon size="small"`
   - `v-icon x-small` → `v-icon size="x-small"`
   - `v-list-item` structure updated to use `<template v-slot:prepend>` for icons
   - `v-expansion-panel` updated for new slot syntax

2. **Breakpoint API**
   - `$vuetify.breakpoint` → `$vuetify.display`

3. **Data Table**
   - `headers` format changed from `{ text, value }` to `{ title, key }`
   - `sort-by` → `:sort-by="[{ key, order }]"`
   - `item-key` remains the same

4. **Form Controls**
   - `v-text-field` and `v-select` updated for new API
   - `append-icon` → `append-inner-icon`
   - `item-value` and `item-text` → `itemValue` and `itemText`

5. **Icons**
   - Icons now imported from `@mdi/js` and used as SVG strings
   - `$icons` global property provides access to icons

### Files Modified

#### Core Files
- `package.json` - Updated dependencies
- `src/main.js` - Vue 3 application entry
- `src/router/index.js` - Vue Router 4 setup
- `src/plugins/store.js` - Vue 3 reactive store
- `src/plugins/vuetify.js` - Vuetify 3 setup
- `src/plugins/icons.js` - Icon plugin updated
- `src/plugins/axios.js` - Axios plugin updated
- `src/plugins/dialog.js` - Dialog plugin updated
- `src/plugins/preference.js` - Preference plugin updated
- `src/plugins/index.js` - Plugin registration updated
- `vite.config.js` - Updated Vite plugin for Vue 3

#### Components
- `src/App.vue` - Updated template structure
- `src/components/LeshanNavBar.vue` - Vuetify 3 updates
- `src/components/ClientInfo.vue` - Vuetify 3 updates
- `src/components/ClientSetting.vue` - Vuetify 3 updates
- `src/components/PromptDialog.vue` - Vuetify 3 updates
- `src/components/RequestButton.vue` - Vuetify 3 updates
- `src/components/instance/InstanceControl.vue` - Vuetify 3 updates
- `src/components/instance/InstanceView.vue` - Vuetify 3 updates
- `src/components/instance/InstanceWriteDialog.vue` - Vuetify 3 updates
- `src/components/instance/InstanceCreateDialog.vue` - Vuetify 3 updates
- `src/components/object/ObjectControl.vue` - Vuetify 3 updates
- `src/components/object/ObjectSelector.vue` - Vuetify 3 updates
- `src/components/resources/ResourceControl.vue` - Vuetify 3 updates
- `src/components/resources/ResourceInstanceControl.vue` - Vuetify 3 updates
- `src/components/resources/ResourceWriteDialog.vue` - Vuetify 3 updates
- `src/components/resources/ResourceInstanceWriteDialog.vue` - Vuetify 3 updates
- `src/components/resources/input/ResourceInput.vue` - Vuetify 3 updates
- `src/components/resources/input/MultiInstanceResourceInput.vue` - Vuetify 3 updates
- `src/components/resources/input/LabelledResourceInput.vue` - Vuetify 3 updates
- `src/components/resources/input/LabelledResourceInstanceInput.vue` - Vuetify 3 updates
- `src/components/resources/view/ResourceView.vue` - Vuetify 3 updates
- `src/components/resources/view/ResourceInstanceView.vue` - Vuetify 3 updates
- `src/components/resources/view/ResourceExpansionPanel.vue` - Vuetify 3 updates
- `src/components/resources/view/ResourceInstanceExpansionPanel.vue` - Vuetify 3 updates
- `src/components/resources/view/MultiInstancesResourceView.vue` - Vuetify 3 updates
- `src/components/resources/view/ResourceDefinitionView.vue` - Vuetify 3 updates
- `src/components/resources/view/SimpleResourceView.vue` - No changes needed
- `src/components/resources/view/SimpleResourceInstanceView.vue` - No changes needed
- `src/components/values/input/SingleValueInput.vue` - Vuetify 3 updates
- `src/components/values/input/BooleanValueInput.vue` - Vuetify 3 updates
- `src/components/compositeOperation/CompositeObjectControl.vue` - Vuetify 3 updates
- `src/components/compositeOperation/CompositeObjectDialog.vue` - Vuetify 3 updates
- `src/components/compositeOperation/CompositeObjectIcons.vue` - No changes needed
- `src/components/compositeOperation/CompositeObjectWriteDialog.vue` - Vuetify 3 updates
- `src/components/compositeOperation/CompositeOperationSettingMenu.vue` - Vuetify 3 updates
- `src/components/compositeOperation/CompositeObjectsSelector.vue` - Vuetify 3 updates

#### Views
- `src/views/Clients.vue` - Vuetify 3 updates, date formatting with dayjs
- `src/views/Client.vue` - Vuetify 3 updates, date formatting with dayjs
- `src/views/Security.vue` - Vuetify 3 updates
- `src/views/Server.vue` - Vuetify 3 updates
- `src/views/About.vue` - Vuetify 3 updates
- `src/views/ObjectView.vue` - Vuetify 3 updates
- `src/views/CompositeOperationView.vue` - Vuetify 3 updates
- `src/views/CompositeObjectView.vue` - Vuetify 3 updates

### Migration Notes

1. **Date Formatting**: Replaced `vue-moment` filter with `dayjs` library for date formatting. All date displays now use `dayjs(date).format('format string')`.

2. **V-Model Syntax**: All custom components using `v-model` have been updated to use `modelValue` prop and `update:modelValue` event emission.

3. **Vuetify Dialog**: The `vuetify-dialog` library has been updated to work with Vuetify 3.

4. **Icon System**: Icons are now imported from `@mdi/js` and used as SVG strings rather than font classes.

5. **Reactivity**: Vue 3's reactivity system automatically handles nested object changes, removing the need for `Vue.set()` calls.

### Testing

After migration, the following should be verified:
- All client registration displays work correctly
- Object/Instance/Resource operations (Read/Write/Observe) function properly
- Composite operations work as expected
- Security information management works correctly
- Date/time displays format correctly
- All dialogs and forms function properly
- Responsive layout works on different screen sizes

### Known Issues

- Some Vuetify 3 components may have slight visual differences from Vuetify 2
- The migration maintains backward compatibility with the existing API endpoints
- Browser caching may need to be cleared after deployment

### Additional Fixes (Post-Migration)

- Fixed `unplugin-vue-components` resolver: Changed from `VuetifyResolver` to `Vuetify3Resolver` in vite.config.js
- Replaced `vuetify-dialog` with `digitspark-dialog` (Vue 3 compatible fork)
- Created custom dialog plugin to replace vuetify-dialog functionality
- Added `@vueuse/core` dependency for `useLocalStorage`
- Updated icon imports to use available icons in @mdi/js v3.x:
  - `mdiCogOutline` → `mdiCogs`
  - `mdiDevices` → `mdiServer`
  - `mdiEyeRemoveOutline` → `mdiEyeOffOutline`
  - `mdiLockOpenRemove` → `mdiLockOpen`
  - `mdiSimOutline` → `mdiSim`
  - `mdiWidgetsOutline` → `mdiWidgets`
  - `mdiDatabaseSyncOutline` → `mdiDatabaseRefresh`
  - `mdiTimerOutline` → `mdiTimer`
  - `mdiTextRecognition` → `mdiTextbox`
  - `mdiGestureTapButton` → `mdiGestureTap`
  - `mdiListStatus` → `mdiFormatListChecks`
  - `mdiTextBoxOutline` → `mdiTextbox`
- Updated Vuetify 3 component API changes:
  - `v-list-item-group` removed, using `v-list-item` directly
  - `v-simple-table` removed, using HTML `<table>` element
  - `v-expansion-panel-header` → `v-expansion-panel-title`
  - `v-expansion-panel-content` → `v-expansion-panel-text`
  - `v-subheader` removed, using HTML `<div>` element
  - `v-list-item-icon` → `prepend` slot
  - `v-list-item-content` → direct content
  - `v-list-item-action` → `append` slot

### Vite 8 Upgrade

- Upgraded Vite from 5.x to 8.2.1
- Upgraded `@vitejs/plugin-vue` from 5.x to 6.x
- Removed `@rollup/plugin-node-resolve` (now built-in to Vite)
- Updated `vite.config.js` to use `import.meta.dirname` compatible syntax
- Added alias configuration for `vuetify/components` and `@mdi/js` to handle Rolldown subpath export resolution
