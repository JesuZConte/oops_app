package com.zconte.oopsapp.di

import com.zconte.oopsapp.data.repository.ExerciseRepositoryImpl
import com.zconte.oopsapp.data.repository.ProgressRepositoryImpl
import com.zconte.oopsapp.domain.repository.ExerciseRepository
import com.zconte.oopsapp.domain.repository.ProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository
}