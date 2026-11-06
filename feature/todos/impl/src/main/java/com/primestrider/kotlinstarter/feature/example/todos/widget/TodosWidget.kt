package com.primestrider.kotlinstarter.feature.example.todos.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.primestrider.kotlinstarter.core.common.AppConfig
import com.primestrider.kotlinstarter.feature.example.todos.data.TodosRepository
import com.primestrider.kotlinstarter.feature.example.todos.model.Todo
import com.primestrider.kotlinstarter.feature.todos.impl.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val NEWEST_SHOWN = 3

/** What the widget shows: how many todos are open and the titles of the newest open ones. */
data class TodosWidgetState(
    val openCount: Int,
    val newest: List<String>,
) {
    companion object {
        /** [todos] come newest first, as [TodosRepository.todos] emits them. */
        fun from(todos: List<Todo>): TodosWidgetState {
            val open = todos.filterNot { it.done }
            return TodosWidgetState(open.size, open.take(NEWEST_SHOWN).map { it.title })
        }
    }
}

/**
 * The home-screen widget. Glance turns this Compose-like code into `RemoteViews` for the launcher, so only Glance
 * composables work here (`androidx.glance.*`, not `androidx.compose.material3`). Tapping it opens Todos through the
 * app's deep link. It is redrawn by [TodosWidgetUpdater] after every change to the todos.
 */
class TodosWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        // A widget is not created by Hilt, so it asks the singleton graph for the repository through an entry point.
        val repository = EntryPointAccessors.fromApplication(context, TodosWidgetEntryPoint::class.java).todosRepository()
        val states = repository.todos.map(TodosWidgetState::from)
        val initial = states.first()
        val open =
            Intent(Intent.ACTION_VIEW, "${AppConfig.appScheme(context)}://example/todos".toUri())
                .setPackage(context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        provideContent {
            val state by states.collectAsState(initial)
            GlanceTheme { TodosWidgetContent(state, GlanceModifier.clickable(actionStartActivity(open))) }
        }
    }
}

@Composable
fun TodosWidgetContent(
    state: TodosWidgetState,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val muted = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
    Column(
        modifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp),
    ) {
        Text(
            context.getString(R.string.todos_title),
            style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
        )
        Text(
            if (state.openCount == 0) {
                context.getString(R.string.todos_empty_title)
            } else {
                context.resources.getQuantityString(R.plurals.todos_widget_open, state.openCount, state.openCount)
            },
            style = muted,
        )
        Spacer(GlanceModifier.height(8.dp))
        state.newest.forEach { title ->
            Text(title, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurface))
        }
    }
}

/** Declared in the manifest; the launcher talks to the widget through it. */
class TodosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodosWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TodosWidgetEntryPoint {
    fun todosRepository(): TodosRepository
}

/** Redraws the placed widgets after the todos change; tests pass a no-op. */
fun interface TodosWidgetUpdater {
    suspend fun update()
}

class GlanceTodosWidgetUpdater
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : TodosWidgetUpdater {
        // The todo is already saved; a widget that fails to redraw must not turn that into an error.
        override suspend fun update() {
            try {
                TodosWidget().updateAll(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("TodosWidget", "Could not update the widget", e)
            }
        }
    }
