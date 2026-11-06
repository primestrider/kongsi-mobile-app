# 7. Cookbook: UI patterns side by side

How to write the everyday UI patterns in Compose, each next to the same thing in React and Vue, with a pointer to where
this repository does it for real. Read chapter 2 first for the Screen / ViewModel split; this chapter is about the code
inside a composable.

| # | Pattern | Repo example |
| --- | --- | --- |
| 1 | [Components and props](#1-components-and-props) | `AppButton` |
| 2 | [Children and slots](#2-children-and-slots) | `AppScaffold`, `DemoSection` |
| 3 | [Local state](#3-local-state) | `pendingDelete` in `TodosScreen` |
| 4 | [Events](#4-events) | `TodoRow`, `AppTextField` |
| 5 | [Conditional rendering](#5-conditional-rendering) | `TodosScreen`, `ProductDetailScreen` |
| 6 | [Lists](#6-lists) | `TodosScreen`, `ProductsScreen` |
| 7 | [Loading, error, empty, success](#7-loading-error-empty-success) | `ProductDetailState`, `ProductsScreen` |
| 8 | [Forms and validation](#8-forms-and-validation) | `FormExampleScreen`, `FormValidation.kt` |
| 9 | [Layout and conditional styles](#9-layout-and-conditional-styles) | everywhere |
| 10 | [Dialogs, bottom sheets, toasts](#10-dialogs-bottom-sheets-toasts) | `ConfirmDialog`, `OptionsBottomSheet`, `Messenger` |
| 11 | [Effects and cleanup](#11-effects-and-cleanup) | `MainActivity`, `AccountNavigation.kt` |
| 12 | [Refs: focus and scroll](#12-refs-focus-and-scroll) | `SearchField` |
| 13 | [Context](#13-context) | `LocalMessenger`, `LocalAppColors` |
| 14 | [Derived values and performance](#14-derived-values-and-performance) | `TodosViewModel`, list keys |
| 15 | [Animation](#15-animation) | the animation feature |
| 16 | [Navigation recap](#16-navigation-recap) | chapter 4 |

The React snippets use function components and hooks; the Vue snippets use `<script setup>` single-file components.

## 1. Components and props

```tsx
// React
type Props = { text: string; onClick: () => void; variant?: 'primary' | 'text'; disabled?: boolean }
export function AppButton({ text, onClick, variant = 'primary', disabled = false }: Props) {
  return <button className={`btn btn-${variant}`} disabled={disabled} onClick={onClick}>{text}</button>
}
```

```vue
<!-- Vue -->
<script setup lang="ts">
withDefaults(defineProps<{ text: string; variant?: 'primary' | 'text'; disabled?: boolean }>(), {
  variant: 'primary', disabled: false,
})
defineEmits<{ click: [] }>()
</script>
<template><button :class="`btn btn-${variant}`" :disabled="disabled" @click="$emit('click')">{{ text }}</button></template>
```

```kotlin
// Compose — core/designsystem/.../component/AppButton.kt (signature)
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    fullWidth: Boolean = false,
)
```

- Props are parameters; default props are default arguments; callers name what they set
  (`AppButton("Add", onClick = onAdd, variant = ButtonVariant.TEXT)`).
- Events are function parameters (`onClick: () -> Unit`), not emitted events.
- `modifier: Modifier = Modifier` is the convention for "let the caller style and position me", like accepting
  `className` / `class` and spreading it on the root element.
- A composable returns nothing (`Unit`): calling it **emits** UI where it is called. There is no `return <jsx/>`.

## 2. Children and slots

```tsx
// React: children
function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return <section><h2>{title}</h2>{children}</section>
}
<Section title="Text fields"><Input /></Section>
```

```vue
<!-- Vue: default slot + named slot -->
<template>
  <section><h2>{{ title }}</h2><slot /><footer><slot name="actions" /></footer></section>
</template>
```

```kotlin
// Compose: content lambdas — core/ui/.../feature/example/ExamplePageScaffold.kt
@Composable
fun DemoSection(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        content()
    }
}

DemoSection(R.string.components_section_text_fields) {   // trailing lambda = the children
    SearchField(query = query, onQueryChange = { query = it })
}
```

- A slot is a parameter of type `@Composable () -> Unit`. A last lambda parameter can be written after the call's
  parentheses, which reads like children.
- Named slots are just more lambda parameters: `AppScaffold(title, actions = { … }, floatingActionButton = { … }) { padding -> … }`.
- `ColumnScope.() -> Unit` lets the children use column-only modifiers (`Modifier.weight`). A slot can also receive
  values: `AppScaffold`'s content gets the `padding` to apply (like a scoped slot / render prop).

## 3. Local state

```tsx
const [pendingDelete, setPendingDelete] = useState<string | null>(null)
```

```vue
<script setup lang="ts">const pendingDelete = ref<string | null>(null)</script>
```

```kotlin
// TodosScreen.kt
var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
// read: pendingDelete     write: pendingDelete = todo.id
```

- `mutableStateOf` is observable state: writing it recomposes the composables that read it.
- `remember { }` keeps it across recompositions (like a hook's state); `rememberSaveable { }` also across rotation and
  process death (it is saved in the activity's saved state).
- `by` is Kotlin delegation, so you read and write `pendingDelete` like a plain variable (no `.value`, no setter).
- Keep **only UI state** here (which dialog is open, an expanded card). Data and business state belong in the
  ViewModel (chapter 2).

## 4. Events

```tsx
<li onClick={() => toggle(todo.id)}>…</li>
<input value={text} onChange={(e) => setText(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && add()} />
```

```vue
<li @click="toggle(todo.id)">…</li>
<input v-model="text" @keyup.enter="add" />
```

```kotlin
// TodosScreen.kt (abridged)
Row(Modifier.toggleable(value = todo.done, role = Role.Checkbox, onValueChange = { onToggle() })) { /* … */ }

AppTextField(
    value = state.input,
    onValueChange = onInputChange,    // controlled input
    imeAction = ImeAction.Done,       // the keyboard's action key shows "Done"
    onImeAction = onAdd,              // …and pressing it adds
    label = stringResource(R.string.todos_placeholder),
)
```

- Click handling is a `Modifier`: `clickable { }`, `toggleable(…)`, `selectable(…)`. They also set the accessibility
  role and state for TalkBack (like using a real `<button>` instead of a clickable `<div>`).
- There is no `event` object: callbacks receive values (`onValueChange: (String) -> Unit`).
- "Enter to submit" is the keyboard's IME action (`ImeAction.Done`, `Next`, `Search`) plus a callback.

## 5. Conditional rendering

```tsx
{loaded && todos.length === 0 && <EmptyState />}
{todos.length > 0 ? <TodoList todos={todos} /> : null}
{pendingDelete && <ConfirmDialog id={pendingDelete} />}
```

```vue
<EmptyState v-if="loaded && todos.length === 0" />
<TodoList v-else-if="todos.length > 0" :todos="todos" />
<ConfirmDialog v-if="pendingDelete" :id="pendingDelete" />
<Spinner v-show="saving" />
```

```kotlin
// TodosScreen.kt
when {
    !state.loaded -> Unit                                   // render nothing yet
    state.todos.isEmpty() -> EmptyState(title = stringResource(R.string.todos_empty_title), /* … */)
    else -> LazyColumn(Modifier.weight(1f)) { /* … */ }
}

pendingDelete?.let { id ->                                  // render only when non-null, with the value
    ConfirmDialog(/* … */ onConfirm = { pendingDelete = null; onRemove(id) }, onDismiss = { pendingDelete = null })
}
```

- Plain Kotlin `if`, `when` and `?.let` decide what is **emitted**. Not calling a composable is the `v-if` / `&&`
  equivalent: it leaves the composition and its `remember` state is lost.
- `when` with a sealed type (pattern 7) is checked by the compiler: forget a case and it does not build.
- `v-show` (keep it, just hide it) has no direct equivalent; keep it composed and hide it visually with
  `Modifier.alpha(0f)` or reserve its space, or animate it with `AnimatedVisibility` (pattern 15).
- `if` can wrap a single modifier too: `Modifier.then(if (selected) Modifier.border(…) else Modifier)`.

## 6. Lists

```tsx
<ul>{todos.map((t) => <TodoRow key={t.id} todo={t} onToggle={() => toggle(t.id)} />)}</ul>
```

```vue
<ul><TodoRow v-for="t in todos" :key="t.id" :todo="t" @toggle="toggle(t.id)" /></ul>
```

```kotlin
// TodosScreen.kt — a long list: only visible rows are composed
LazyColumn(Modifier.weight(1f)) {
    items(state.todos, key = { it.id }) { todo ->
        TodoRow(todo, onToggle = { onToggle(todo.id) }, onDelete = { pendingDelete = todo.id })
    }
}

// a short, fixed list: plain forEach inside a Column
Column { options.forEach { option -> OptionRow(option) } }
```

- `LazyColumn` / `LazyRow` / `LazyVerticalGrid` are virtualized (like react-window / vue-virtual-scroller), and they are
  the normal way to show a list. `Column { forEach }` composes everything, fine for a handful of items.
- `key = { it.id }` is React's `key` / Vue's `:key`: it keeps each row's state and scroll position when items move.
- Infinite scroll with a server: Paging 3. `ProductsScreen` renders
  `items(count = products.itemCount, key = products.itemKey { it.id })` and the library loads the next page as you scroll
  (like `useInfiniteQuery` + an intersection observer, built in).
- Row animations when items are added, moved or removed: `Modifier.animateItem()` on the row (the animation feature's
  lists page).

## 7. Loading, error, empty, success

```tsx
// React with TanStack Query
const { data, isLoading, error, refetch } = useQuery({ queryKey: ['product', id], queryFn: () => getProduct(id) })
if (isLoading) return <Spinner />
if (error) return <ErrorState message={error.message} onRetry={refetch} />
if (!data) return <EmptyState />
return <ProductDetail product={data} />
```

```vue
<template>
  <Spinner v-if="isLoading" />
  <ErrorState v-else-if="error" :message="error.message" @retry="refetch" />
  <EmptyState v-else-if="!data" />
  <ProductDetail v-else :product="data" />
</template>
```

```kotlin
// ProductDetailViewModel.kt — one state, exactly one case at a time
sealed interface ProductDetailState {
    data object Loading : ProductDetailState
    data class Loaded(val product: Product) : ProductDetailState
    data object NotFound : ProductDetailState
    data class Failed(val error: ApiError) : ProductDetailState
}

// ProductDetailScreen.kt
when (state) {
    ProductDetailState.Loading -> LoadingState(content)
    ProductDetailState.NotFound -> EmptyState(stringResource(R.string.products_not_found), content)
    is ProductDetailState.Failed -> ErrorState(state.error.userMessage(), onRetry, content)
    is ProductDetailState.Loaded -> ProductDetail(state.product, onAddToCart, content)
}
```

- A `sealed interface` makes impossible states impossible: never "loading and error at once", never a product while
  failed. The compiler forces the screen to handle each case (like a TypeScript discriminated union with exhaustive
  `switch`).
- The ViewModel produces the state; the screen only switches on it. `LoadingState`, `ErrorState`, `EmptyState` are shared
  design-system components.
- Paged lists read the library's states instead: `ProductsScreen` switches on `products.loadState.refresh`
  (`LoadState.Loading` / `LoadState.Error`) and `itemCount`, shows a skeleton while loading, keeps old items with an
  inline error when a refresh fails, and supports pull-to-refresh.

## 8. Forms and validation

```tsx
// React Hook Form + zod (typical)
const { register, handleSubmit, formState: { errors } } = useForm({ resolver: zodResolver(schema) })
<form onSubmit={handleSubmit(onSubmit)}>
  <input {...register('email')} /> {errors.email && <span>{errors.email.message}</span>}
</form>
```

```vue
<!-- VeeValidate / plain refs + computed errors -->
<input v-model="email" /> <span v-if="errors.email">{{ errors.email }}</span>
<button :disabled="submitting" @click="submit">Send</button>
```

```kotlin
// FormValidation.kt — validation is a pure function, testable without UI
fun validate(values: FormValues): Map<FormField, Int> =
    buildMap {
        if (values.name.isBlank()) put(FormField.NAME, R.string.form_error_name_required)
        // … email, phone, password, bio rules
    }

// FormExampleScreen.kt (abridged) — each field shows its error, the keyboard moves to the next field
AppTextField(
    value = state.values.email,
    onValueChange = { onChange(FormField.EMAIL, it) },
    label = stringResource(R.string.form_field_email),
    placeholder = stringResource(R.string.form_field_email_placeholder),
    errorText = errorText(state, FormField.EMAIL),
    keyboardType = KeyboardType.Email,
    imeAction = ImeAction.Next,
)
AppButton(
    text = stringResource(if (state.submitting) R.string.form_submitting else R.string.form_submit),
    onClick = onSubmit,
    loading = state.submitting,     // disables the button: no double submit
)
```

- Inputs are controlled: the value comes from state, every keystroke calls back (React's `value` + `onChange`; Vue's
  `v-model` is the same pair).
- `validate()` returns a map of field → error message resource; the ViewModel validates on submit and again on change
  once submitted (`FormExampleViewModel`). Error messages are string resources, so they are translated.
- `keyboardType` picks the keyboard (email, phone, number); `imeAction = Next` moves focus to the next field like Tab.
- There is no `<form>` element and no native submit: the button calls `onSubmit`.

## 9. Layout and conditional styles

```tsx
<div className="flex flex-col gap-3 p-4">
  <div className="flex items-center gap-2"><Avatar /><span className={done ? 'line-through text-muted' : ''}>{title}</span></div>
</div>
```

```vue
<div class="col gap-3 p-4"><span :class="{ 'line-through text-muted': done }">{{ title }}</span></div>
```

```kotlin
Column(Modifier.padding(Spacing.s4), verticalArrangement = Arrangement.spacedBy(Spacing.s3)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s2)) {
        Avatar(name = user.name)
        Text(
            title,
            textDecoration = if (done) TextDecoration.LineThrough else null,
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}
```

| CSS / Tailwind | Compose |
| --- | --- |
| `display: flex; flex-direction: column` | `Column` |
| `flex-direction: row` | `Row` |
| `position: relative` + absolute children | `Box` (children stack; `Modifier.align(…)`) |
| `gap` | `Arrangement.spacedBy(…)` |
| `align-items` / `justify-content` | `verticalAlignment`/`horizontalAlignment`, `Arrangement` |
| `flex: 1` | `Modifier.weight(1f)` |
| `padding`, `width`, `height`, `background`, `border-radius` | `Modifier.padding()`, `.width()`, `.height()`, `.background(color, shape)`, `.clip(shape)` |
| `overflow: auto` | `Modifier.verticalScroll(rememberScrollState())` or a lazy list |
| class toggling | `if` inside parameters or `Modifier.then(if (…) … else Modifier)` |
| media queries | `LocalConfiguration.current.screenWidthDp`, window size classes |

- Modifier **order matters**: `padding().background()` paints the background inside the padding,
  `background().padding()` outside it (unlike CSS, where order of properties does not matter).
- Colours, spacing and shapes come from the design system tokens (`Spacing`, `Radii`, `MaterialTheme.colorScheme`),
  never literals (chapter 6, "Styling and theming").

## 10. Dialogs, bottom sheets, toasts

```tsx
// React: portal-based dialog, toast library
{open && createPortal(<Dialog onClose={() => setOpen(false)} />, document.body)}
toast.success('Todo added')
```

```vue
<Teleport to="body"><Dialog v-if="open" @close="open = false" /></Teleport>
<!-- toast via a plugin: useToast().success('Todo added') -->
```

```kotlin
// A dialog is shown by composing it; dismissing sets the state back (TodosScreen.kt)
pendingDelete?.let { id ->
    ConfirmDialog(
        title = stringResource(R.string.todos_remove_title),
        message = stringResource(R.string.todos_remove_description),
        confirmLabel = stringResource(R.string.todos_delete),
        onConfirm = { pendingDelete = null; onRemove(id) },
        onDismiss = { pendingDelete = null },
        destructive = true,
    )
}

// A toast/snackbar from a ViewModel, wherever the user is (TodosViewModel.kt)
messenger.success(UiText.Resource(R.string.todos_added))
```

- Dialogs and sheets (`ConfirmDialog`, `AppBottomSheet`, `OptionsBottomSheet` in `core:designsystem`) render in their
  own window above the screen, so no portal/teleport is needed: composing them shows them, not composing them hides
  them.
- The back button and a tap outside call `onDismiss`; you decide what state to reset.
- Snackbars go through the app-wide `Messenger` (`MessengerHost` in `MainShell` shows them). ViewModels call it with a
  `UiText` (a string resource), so messages are translated and survive the screen being recreated.

## 11. Effects and cleanup

```tsx
useEffect(() => {
  const listener = (e: Event) => handle(e)
  window.addEventListener('message', listener)
  return () => window.removeEventListener('message', listener)    // cleanup
}, [handle])
```

```vue
<script setup lang="ts">
onMounted(() => window.addEventListener('message', handle))
onUnmounted(() => window.removeEventListener('message', handle))
watch(session, (s) => { if (!s) router.replace('/sign-in') })
</script>
```

```kotlin
// MainActivity.kt — subscribe with cleanup
DisposableEffect(navController) {
    val listener = Consumer<Intent> { navController.handleDeepLink(it) }
    addOnNewIntentListener(listener)
    onDispose { removeOnNewIntentListener(listener) }
}

// AccountNavigation.kt — react to state changes (like watch / useEffect with deps)
LaunchedEffect(loaded, current == null, signingOut) {
    if (loaded && current == null && !signingOut) {
        navController.navigate(SignInRoute) { popUpTo<AccountRoute> { inclusive = true } }
    }
}

// collect one-off events from a ViewModel
LaunchedEffect(viewModel) {
    viewModel.signedOut.collect { navController.popBackStack(ProfileRoute, inclusive = false) }
}
```

- `LaunchedEffect(keys) { }` runs a coroutine when the composable enters, and restarts it when a key changes; it is
  cancelled when the composable leaves (no manual cleanup for async work).
- `DisposableEffect(keys) { …; onDispose { } }` is for non-coroutine subscriptions that need cleanup.
- Do **not** fetch data in an effect: rotation would refetch. The ViewModel loads data (chapter 2).

## 12. Refs: focus and scroll

```tsx
const inputRef = useRef<HTMLInputElement>(null)
inputRef.current?.focus()
listRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
```

```vue
<input ref="input" />  <!-- const input = ref<HTMLInputElement>(); input.value?.focus() -->
```

```kotlin
// Focus: a FocusRequester attached with a modifier
val focus = remember { FocusRequester() }
AppTextField(value, onValueChange, label = "Name", modifier = Modifier.focusRequester(focus))
LaunchedEffect(Unit) { focus.requestFocus() }

// Hide the keyboard / drop focus (SearchField does this on the keyboard's Search action)
val focusManager = LocalFocusManager.current
focusManager.clearFocus()

// Scroll: hoist the list's state and drive it
val listState = rememberLazyListState()
LazyColumn(state = listState) { /* … */ }
val scope = rememberCoroutineScope()
scope.launch { listState.animateScrollToItem(0) }
```

- There are no DOM nodes to grab. Instead you create a controller object (`FocusRequester`, `LazyListState`,
  `ScrollState`), pass it in, and call it.
- In this repository only `SearchField` (`core/designsystem/src/main/java/com/primestrider/kotlinstarter/core/designsystem/component/TextFields.kt`)
  manages focus; the focus and scroll snippets above are the standard APIs to use when you need them.

## 13. Context

```tsx
const ThemeContext = createContext(lightColors)
<ThemeContext.Provider value={dark ? darkColors : lightColors}>…</ThemeContext.Provider>
const colors = useContext(ThemeContext)
```

```vue
provide('messenger', messenger)          // ancestor
const messenger = inject('messenger')!   // descendant
```

```kotlin
// MainActivity.kt — provide
CompositionLocalProvider(LocalMessenger provides messenger, LocalPictureInPictureHost provides pictureInPictureHost) {
    MainShell(navController)
}

// any composable below — read
val messenger = LocalMessenger.current
val colors = LocalAppColors.current       // provided by AppTheme
```

- `CompositionLocal` is for UI-tree values: theme tokens, the messenger, the PiP host.
- Services (repositories, the API client, the session) are **not** passed through the tree: Hilt injects them into
  ViewModels and classes (chapter 3). In web terms: context for UI concerns, a DI container for services.

## 14. Derived values and performance

```tsx
const visible = useMemo(() => todos.filter((t) => filter === 'all' || t.done === (filter === 'done')), [todos, filter])
const MemoRow = React.memo(TodoRow)
```

```vue
const visible = computed(() => todos.value.filter((t) => filter.value === 'all' || t.done === (filter.value === 'done')))
```

```kotlin
// Prefer deriving in the ViewModel (TodosViewModel.kt): the filtered list and counters are computed once per change
combine(inputs, filterFlow, repository.todos) { input, filter, all -> TodosUiState(todos = all.filtered(filter), /* … */) }

// In a composable: cache an expensive computation until its inputs change
val sorted = remember(items) { items.sortedBy { it.title } }

// Recompose only when a derived boolean flips, not on every scroll pixel
val showToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
```

- Compose skips a composable when its parameters are unchanged (like `React.memo` by default) as long as the types are
  stable: immutable `data class` UI state, `List` from Kotlin, lambdas that do not capture new objects each time.
- Give lazy list items a `key` (pattern 6). Read state as late as possible (pass `() -> Float` instead of `Float` for
  values that change every frame).

## 15. Animation

```tsx
// CSS transition / Framer Motion
<div style={{ transform: `scale(${pressed ? 0.96 : 1})`, transition: 'transform 150ms' }} />
<AnimatePresence>{open && <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} />}</AnimatePresence>
```

```vue
<Transition name="fade"><Panel v-if="open" /></Transition>
```

```kotlin
// Animate a value when its target changes (animation/cards/CardsScreen.kt uses this for press feedback)
val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale })

// Enter/exit: v-if with a transition (animation/transitions/PredictiveBackDemo.kt)
AnimatedVisibility(open, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut() + scaleOut(targetScale = 0.9f)) {
    Panel()
}

// Size changes (expand/collapse) — CardsScreen.kt
Column(Modifier.animateContentSize()) { if (expanded) Details() }

// Swap content with a transition
AnimatedContent(targetState = count) { value -> Text("$value") }
```

- Animations are state-driven: change the target value, Compose animates towards it. No class toggling or timers.
- The animation feature (`feature/animation/impl`) has a page per pattern with the API name shown next to each example;
  `docs/examples.md` describes them. All of them respect the system "Remove animations" setting.

## 16. Navigation recap

| Task | React Router | Vue Router | This app (chapter 4) |
| --- | --- | --- | --- |
| Declare a page | `{ path: '/products/:id', element }` | `{ path, component, name }` | `@Serializable data class ProductDetailRoute(val id: Long)` + `composable<ProductDetailRoute> { }` |
| Go to a page | `navigate('/products/42')` | `router.push({ name: 'product', params: { id: 42 } })` | `navController.navigate(ProductDetailRoute(42))` |
| Read params | `useParams()` | `route.params` | `entry.toRoute<ProductDetailRoute>().id`, or `SavedStateHandle` in the ViewModel |
| Back | `navigate(-1)` | `router.back()` | `navController.popBackStack()` (the system back gesture does it too) |
| Replace (no history entry) | `navigate(to, { replace: true })` | `router.replace(to)` | `navigate(route) { popUpTo<Current> { inclusive = true } }` |
| Guard | `<RequireAuth>` / loader redirect | `beforeEach` | a `LaunchedEffect` in the destination (`AccountNavigation.kt`) |
| Nested layouts | `<Outlet/>` | `<router-view>` | nested graphs per tab inside `MainShell` |
| Deep link | the URL itself | the URL itself | `navDeepLink<Route>(basePath = "$appScheme://example/…")` + the manifest intent filter |

## Tip: previews instead of Storybook

Where a React or Vue project keeps stories in Storybook, Compose keeps previews next to the code: a private
`@ThemePreviews` function at the end of the file renders the composable in light and dark in Android Studio's design
pane, with no app run. Every screen and component here has one (`PreviewCoverageTest` checks), so the quickest way to
see a pattern from this chapter is to open its file and look at the preview.

Back to the [index](README.md).
