package com.zconte.oopsapp.domain.usecase

import kotlin.math.roundToInt

private const val MINUTES_PER_QUESTION = 1.8

/**
 * Total time budget for a checkpoint attempt, in seconds -- the real 1Z0-830's pace (50 questions
 * / 90 minutes = 1.8 min/question), applied as one lump sum rather than a per-question timer so a
 * parsons/predict_output question (which naturally takes longer to read) isn't penalized against
 * an mcq.
 */
fun computeCheckpointTimeBudgetSeconds(questionCount: Int): Int =
    (questionCount * MINUTES_PER_QUESTION).roundToInt() * 60
