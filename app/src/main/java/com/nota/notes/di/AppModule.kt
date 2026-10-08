package com.nota.notes.di

import android.content.Context
import com.nota.notes.data.AccountStore
import com.nota.notes.data.DbHolder
import com.nota.notes.data.FoldersRepository
import com.nota.notes.data.NotesRepository
import com.nota.notes.security.AuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun store(@ApplicationContext ctx: Context): com.nota.notes.security.PassphraseStore =
        com.nota.notes.security.PassphraseStore(ctx)

    // DbHolder يُبنى عبر @Inject — لا runBlocking هنا (الفتح معلّق على IO)
    @Provides @Singleton fun repo(holder: DbHolder, @ApplicationContext ctx: Context, account: AccountStore): NotesRepository = NotesRepository(holder, ctx, account)
    @Provides @Singleton fun foldersRepo(holder: DbHolder, account: AccountStore): FoldersRepository = FoldersRepository(holder, account)
    @Provides @Singleton fun auth(@ApplicationContext ctx: Context): AuthManager = AuthManager(ctx)

    // المرحلة 0 — الهوية: مخزن الحساب (روابط Billing/Entitlements تأتي من نكهة mock/play)
    @Provides @Singleton fun account(@ApplicationContext ctx: Context): AccountStore = AccountStore(ctx)
}
