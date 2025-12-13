package com.example.gestaobilhares.data.di

import android.content.Context
import com.example.gestaobilhares.data.database.AppDatabase
import com.example.gestaobilhares.data.dao.*
import com.example.gestaobilhares.data.repository.AppRepository
// import com.example.gestaobilhares.data.repository.domain.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    // Providers de repositórios de domínio necessários para ViewModels migrados para Hilt
    @Provides
    @Singleton
    fun provideClienteRepository(
        dao: ClienteDao,
        appRepository: AppRepository
    ): com.example.gestaobilhares.data.repository.ClienteRepository {
        return com.example.gestaobilhares.data.repository.ClienteRepository(dao, appRepository)
    }

    @Provides
    @Singleton
    fun provideAcertoRepository(
        acertoDao: AcertoDao,
        clienteDao: ClienteDao
    ): com.example.gestaobilhares.data.repository.AcertoRepository {
        return com.example.gestaobilhares.data.repository.AcertoRepository(acertoDao, clienteDao)
    }

    @Provides
    @Singleton
    fun provideCicloAcertoRepository(
        cicloDao: CicloAcertoDao,
        despesaDao: DespesaDao,
        acertoRepository: com.example.gestaobilhares.data.repository.AcertoRepository,
        clienteRepository: com.example.gestaobilhares.data.repository.ClienteRepository,
        rotaDao: RotaDao,
        colaboradorDao: ColaboradorDao
    ): com.example.gestaobilhares.data.repository.CicloAcertoRepository {
        return com.example.gestaobilhares.data.repository.CicloAcertoRepository(
            cicloDao, despesaDao, acertoRepository, clienteRepository, rotaDao, colaboradorDao
        )
    }

    /*
    // TODO: Outros repositórios de domínio (descomentar conforme necessário)
    @Provides
    @Singleton
    fun provideRotaRepository(dao: RotaDao, firestore: FirebaseFirestore): RotaRepository {
        return RotaRepository(dao, firestore)
    }
    
    @Provides
    @Singleton
    fun provideDespesaRepository(dao: DespesaDao, firestore: FirebaseFirestore): DespesaRepository {
        return DespesaRepository(dao, firestore)
    }
    
    @Provides
    @Singleton
    fun provideColaboradorRepository(dao: ColaboradorDao, firestore: FirebaseFirestore): ColaboradorRepository {
        return ColaboradorRepository(dao, firestore)
    }
    
    @Provides
    @Singleton
    fun provideCicloRepository(dao: CicloAcertoDao, firestore: FirebaseFirestore): CicloRepository {
        return CicloRepository(dao, firestore)
    }
    
    @Provides
    @Singleton
    fun provideMesaRepository(dao: MesaDao, firestore: FirebaseFirestore): MesaRepository {
        return MesaRepository(dao, firestore)
    }
    
    @Provides
    @Singleton
    fun provideVeiculoRepository(dao: VeiculoDao, firestore: FirebaseFirestore): VeiculoRepository {
        return VeiculoRepository(dao, firestore)
    }
    
    @Provides
    @Singleton
    fun provideContratoRepository(dao: ContratoDao, firestore: FirebaseFirestore): ContratoRepository {
        return ContratoRepository(dao, firestore)
    }
    
    @Provides
    @Singleton
    fun provideMetaRepository(dao: MetaDao, firestore: FirebaseFirestore): MetaRepository {
        return MetaRepository(dao, firestore)
    }
    
    @Provides
    @Singleton
    fun provideEquipmentRepository(dao: EquipmentDao, firestore: FirebaseFirestore): EquipmentRepository {
        return EquipmentRepository(dao, firestore)
    }

    @Provides
    @Singleton
    fun provideMetaColaboradorRepository(dao: MetaColaboradorDao, firestore: FirebaseFirestore): MetaColaboradorRepository {
        return MetaColaboradorRepository(dao, firestore)
    }

    @Provides
    @Singleton
    fun providePanoRepository(dao: PanoDao, firestore: FirebaseFirestore): PanoRepository {
        return PanoRepository(dao, firestore)
    }
    */
    
    @Provides
    @Singleton
    fun provideAppRepository(
        database: AppDatabase
    ): AppRepository {
        return AppRepository.create(database)
    }
}
