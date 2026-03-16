package myedu.oshsu.kg.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import myedu.oshsu.kg.MainViewModel
import myedu.oshsu.kg.MoocStep
import myedu.oshsu.kg.MoocTestAnswer
import myedu.oshsu.kg.R
import myedu.oshsu.kg.ui.components.ThemedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoocLessonScreen(vm: MainViewModel, onClose: () -> Unit) {
    val lesson = vm.selectedMoocLesson

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = lesson?.title ?: stringResource(R.string.mooc_lesson),
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
                vm.isMoocStepsLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                vm.moocLessonSteps.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.LibraryBooks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.mooc_no_steps),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val sortedSteps = vm.moocLessonSteps.sortedBy { it.step }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedSteps, key = { it.id }) { step ->
                            MoocStepCard(
                                step = step,
                                streamId = vm.selectedMoocStreamId ?: 0,
                                glassmorphismEnabled = vm.glassmorphismEnabled,
                                onSubmitAnswer = { stepId, streamId, answerId ->
                                    vm.submitMoocTestAnswer(stepId, streamId, answerId)
                                },
                                onMarkCompleted = { stepId, streamId ->
                                    vm.markMoocStepCompleted(stepId, streamId)
                                }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }

                    // Show test result snackbar
                    vm.moocTestResult?.let { msg ->
                        Snackbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            action = {
                                TextButton(onClick = { vm.moocTestResult = null }) {
                                    Text("OK")
                                }
                            }
                        ) {
                            Text(msg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoocStepCard(
    step: MoocStep,
    streamId: Int,
    glassmorphismEnabled: Boolean,
    onSubmitAnswer: (Int, Int, Int) -> Unit,
    onMarkCompleted: (Int, Int) -> Unit
) {
    val stepTypeName = step.type?.title ?: step.type?.name ?: ""
    val isTest = stepTypeName.contains("test", ignoreCase = true) ||
                 step.content?.answers?.isNotEmpty() == true
    val isDocument = stepTypeName.contains("document", ignoreCase = true) ||
                     step.content?.document != null
    val isVideo = stepTypeName.contains("video", ignoreCase = true) ||
                  step.content?.video_url != null

    val stepIcon: ImageVector = when {
        isTest -> Icons.Outlined.Quiz
        isDocument -> Icons.Outlined.Description
        isVideo -> Icons.Outlined.PlayCircle
        else -> Icons.Outlined.Article
    }

    val completedColor = if (step.chills == true)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.surfaceContainer

    ThemedCard(
        modifier = Modifier.fillMaxWidth(),
        materialColor = completedColor,
        glassmorphismEnabled = glassmorphismEnabled
    ) {
        Column {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    stepIcon,
                    contentDescription = null,
                    tint = if (step.chills == true) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${stringResource(R.string.mooc_step)} ${step.step ?: 0}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (step.score != null && step.score > 0) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${step.my_score ?: 0}/${step.score}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (step.chills == true) {
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Content
            val title = step.content?.title
            val description = step.content?.description
            val textContent = step.content?.content

            if (title != null && title.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (description != null && description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Test content
            if (isTest && step.content?.answers?.isNotEmpty() == true) {
                Spacer(Modifier.height(12.dp))

                if (textContent != null && textContent.isNotBlank()) {
                    Text(
                        text = textContent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                }

                TestAnswerSection(
                    answers = step.content.answers,
                    isCompleted = step.chills == true,
                    onSubmit = { answerId ->
                        onSubmitAnswer(step.id, streamId, answerId)
                    }
                )
            }

            // Document info
            if (isDocument && step.content?.document != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = step.content.document,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Mark as read button for non-test steps
            if (!isTest && step.chills != true) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { onMarkCompleted(step.id, streamId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.mooc_mark_read))
                }
            }
        }
    }
}

@Composable
private fun TestAnswerSection(
    answers: List<MoocTestAnswer>,
    isCompleted: Boolean,
    onSubmit: (Int) -> Unit
) {
    var selectedAnswerId by remember { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        answers.forEach { answer ->
            val isSelected = selectedAnswerId == answer.id
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        onClick = { if (!isCompleted) selectedAnswerId = answer.id },
                        role = Role.RadioButton
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        enabled = !isCompleted
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = answer.text ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (!isCompleted && selectedAnswerId != null) {
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { selectedAnswerId?.let { onSubmit(it) } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.mooc_submit_answer))
            }
        }
    }
}
