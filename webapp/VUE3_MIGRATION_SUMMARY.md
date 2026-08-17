# Vue 2 to Vue 3 / Vuetify 2 to Vuetify 3 Migration Summary

## Overview
This document summarizes the changes made to convert the Vue.js application from Vue 2/Vuetify 2 syntax to Vue 3/Vuetify 3 syntax.

## Files Modified

### Views
1. **views/Client.vue**
   - `x-large` → `size="x-large"`
   - `grey lighten-5` → `grey-lighten-5`
   - `dense` on v-list → removed

2. **views/Clients.vue**
   - `dense` on v-data-table → removed
   - `flat` on v-toolbar → removed

3. **views/Security.vue**
   - `flat` on v-toolbar → removed
   - `dark` on v-btn → `color="dark"`
   - `dark` on v-icon → `color="dark"`
   - `small` on v-icon → `size="small"`
   - `dense` on v-data-table → removed

4. **views/CompositeObjectView.vue**
   - `grey lighten-5` → `grey-lighten-5`
   - `small` on v-icon → `size="small"`
   - `grey--text` → `grey`
   - `text--darken-1` → `text-darken-1`
   - `dense` and `tile` on v-expansion-panels → removed

5. **views/CompositeOperationView.vue**
   - `grey lighten-5` → `grey-lighten-5`

6. **views/ObjectView.vue**
   - `grey lighten-5` → `grey-lighten-5`

### Components - Resources
7. **components/resources/view/ResourceExpansionPanel.vue**
   - `v-expansion-panel-header` → `v-expansion-panel-title`
   - `v-expansion-panel-content` → `v-expansion-panel-text`
   - `text--disabled` → `text-disabled`
   - `x-small` on v-icon → `size="x-small"`
   - `!open` → `!expanded`
   - Added missing `<style scoped>` block with `:deep()` selector

8. **components/resources/view/ResourceInstanceExpansionPanel.vue**
   - `v-expansion-panel-header` → `v-expansion-panel-title`
   - `v-expansion-panel-content` → `v-expansion-panel-text`
   - `text--disabled` → `text-disabled`
   - `x-small` on v-icon → `size="x-small"`
   - `!open` → `!expanded`

9. **components/resources/view/MultiInstancesResourceView.vue**
   - `text--disabled` → `text-disabled`
   - `x-small` on v-icon → `size="x-small"`
   - `dense` on v-simple-table → removed

10. **components/resources/view/ResourceInstanceView.vue**
    - `grey lighten-5` → `grey-lighten-5`
    - `text--primary` → `text-primary`

11. **components/resources/view/ResourceView.vue**
    - `grey lighten-5` → `grey-lighten-5`
    - `text--primary` → `text-primary`

12. **components/resources/view/ResourceDefinitionView.vue**
    - `grey lighten-4` → `grey-lighten-4`
    - `dense` on v-simple-table → removed

13. **components/resources/input/MultiInstanceResourceInput.vue**
    - `grey lighten-4` → `grey-lighten-4`
    - `small` on v-btn → `size="small"`
    - `dense` on v-row → removed
    - `icon` on v-btn → `variant="text"`

14. **components/resources/ResourceControl.vue**
    - `dense small` on v-icon → `size="small"`

15. **components/resources/ResourceInstanceControl.vue**
    - `dense small` on v-icon → `size="small"`

16. **components/resources/ResourceWriteDialog.vue**
    - `hide-overlay` → `overlay="false"`
    - `grey lighten-2` → `grey-lighten-2`

17. **components/resources/ResourceInstanceWriteDialog.vue**
    - `hide-overlay` → `overlay="false"`
    - `grey lighten-2` → `grey-lighten-2`

18. **components/resources/input/LabelledResourceInput.vue**
    - `dense` on v-row → removed

19. **components/resources/input/LabelledResourceInstanceInput.vue**
    - `dense` on v-row → removed

### Components - Instance
20. **components/instance/InstanceCreateDialog.vue**
    - `hide-overlay` → `overlay="false"`
    - `grey lighten-2` → `grey-lighten-2`
    - `dense` on v-row → removed

21. **components/instance/InstanceWriteDialog.vue**
    - `hide-overlay` → `overlay="false"`
    - `grey lighten-2` → `grey-lighten-2`

22. **components/instance/InstanceControl.vue**
    - `dense small` on v-icon → `size="small"`

23. **components/instance/InstanceView.vue**
    - `small` on v-icon → `size="small"`
    - `dense` and `tile` on v-expansion-panels → removed

### Components - Object
24. **components/object/ObjectSelector.vue**
    - `dense` on v-list → removed
    - `small` on v-icon → `size="small"`

### Components - Composite Operation
25. **components/compositeOperation/CompositeObjectControl.vue**
    - `dense small` on v-icon → `size="small"`
    - `small` on v-btn → `size="small"`
    - `tile` on v-btn → removed
    - `small` on v-icon → `size="small"`

26. **components/compositeOperation/CompositeObjectsSelector.vue**
    - `grey lighten-5` → `grey-lighten-5`
    - `small` on v-btn → `size="small"`
    - `dense` on v-list → removed
    - `icon` on v-btn → `variant="text"`
    - `small` on v-icon → `size="small"`

27. **components/compositeOperation/CompositeObjectDialog.vue**
    - `hide-overlay` → `overlay="false"`
    - `grey lighten-2` → `grey-lighten-2`

28. **components/compositeOperation/CompositeObjectWriteDialog.vue**
    - `hide-overlay` → `overlay="false"`
    - `grey lighten-2` → `grey-lighten-2`

29. **components/compositeOperation/CompositeOperationSettingMenu.vue**
    - `dense` on v-select → removed

### Components - Other
30. **components/ClientSetting.vue**
    - `dense` on v-select → removed (3 occurrences)

31. **components/LeshanNavBar.vue**
    - `dark flat dense` on v-app-bar → `color="dark"`

## Summary of Changes

### Class Name Changes (Vuetify 2 → Vuetify 3)
| Vue 2/Vuetify 2 | Vue 3/Vuetify 3 |
|-----------------|-----------------|
| `text--disabled` | `text-disabled` |
| `text--primary` | `text-primary` |
| `grey--text` | `grey` |
| `text--darken-1` | `text-darken-1` |
| `grey lighten-2` | `grey-lighten-2` |
| `grey lighten-4` | `grey-lighten-4` |
| `grey lighten-5` | `grey-lighten-5` |

### Component Prop Changes
| Vue 2/Vuetify 2 | Vue 3/Vuetify 3 |
|-----------------|-----------------|
| `x-small` (on v-icon) | `size="x-small"` |
| `x-large` (on v-icon) | `size="x-large"` |
| `small` (on v-icon) | `size="small"` |
| `dark` (on v-btn/v-icon) | `color="dark"` |
| `flat` (on v-toolbar/v-app-bar) | removed |
| `dense` (on v-data-table/v-list/v-simple-table/v-row/v-select) | removed |
| `tile` (on v-btn/v-expansion-panels) | removed |
| `hide-overlay` (on v-dialog) | `overlay="false"` |
| `icon` (on v-btn) | `variant="text"` |

### Component Name Changes
| Vue 2/Vuetify 2 | Vue 3/Vuetify 3 |
|-----------------|-----------------|
| `v-expansion-panel-header` | `v-expansion-panel-title` |
| `v-expansion-panel-content` | `v-expansion-panel-text` |

### Slot Prop Changes
| Vue 2/Vuetify 2 | Vue 3/Vuetify 3 |
|-----------------|-----------------|
| `open` (in v-expansion-panel-header slot) | `expanded` |

### Style Changes
- Added missing `<style scoped>` block with `:deep()` selector in `ResourceExpansionPanel.vue`

## Testing Recommendations
1. Test all expansion panels to ensure they expand/collapse correctly
2. Test all dialogs to ensure they open/close correctly
3. Test all icon buttons to ensure they display correctly
4. Test all color classes to ensure text colors are correct
5. Test all list components to ensure they render correctly
6. Test all data tables to ensure they render correctly
