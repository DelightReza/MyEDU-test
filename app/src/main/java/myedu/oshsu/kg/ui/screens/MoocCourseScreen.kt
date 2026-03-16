package myedu.oshsu.kg.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.MoocLesson
import myedu.oshsu.kg.R
import myedu.oshsu.kg.ui.components.ThemedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoocCourseScreen(vm: MainViewModel, onClose: () -> Unit) {
    val courseItem = vm.selectedMoocCourse

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = courseItem?.subjectName ?: stringResource(R.string.mooc_course),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.desc_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                vm.isMoocLessonsLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                vm.moocCourseLessons.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.mooc_no_lessons),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (course in vm.moocCourseLessons) {
                            // Course header with description
                            item(key = "course_${course.id}") {
                                if (course.description?.isNotBlank() == true) {
                                    ThemedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        materialColor = MaterialTheme.colorScheme.primaryContainer,
                                        glassmorphismEnabled = vm.glassmorphismEnabled
                                    ) {
                                        Column {
                                            Text(
                                                text = course.title ?: stringResource(R.string.mooc_course),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            if (course.user != null) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = course.user.getFullName(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = course.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                                maxLines = 4,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }

                            val lessons = course.lessons
                                ?.filter { it.is_published == true && it.is_deleted != true }
                                ?.sortedBy { it.sequence_number } ?: emptyList()

                            items(lessons, key = { it.id }) { lesson ->
                                // Pick the first stream ID from the selected course
                                val streamId = courseItem?.streamIds?.lastOrNull() ?: 0
                                MoocLessonCard(
                                    lesson = lesson,
                                    glassmorphismEnabled = vm.glassmorphismEnabled,
                                    onClick = {
                                        vm.selectedMoocLesson = lesson
                                        vm.loadMoocSteps(lesson.id, streamId)
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoocLessonCard(
    lesson: MoocLesson,
    glassmorphismEnabled: Boolean,
    onClick: () -> Unit
) {
    ThemedCard(
        modifier = Modifier.fillMaxWidth(),
        materialColor = MaterialTheme.colorScheme.surfaceContainer,
        glassmorphismEnabled = glassmorphismEnabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lesson number badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (lesson.active == true) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (lesson.sequence_number ?: 0).toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (lesson.active == true) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title ?: "-",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (lesson.from != null && lesson.to != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${lesson.from} — ${lesson.to}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
