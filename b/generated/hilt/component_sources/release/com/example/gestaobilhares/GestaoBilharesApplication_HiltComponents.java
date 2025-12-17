package com.example.gestaobilhares;

import com.example.gestaobilhares.core.di.CoreModule;
import com.example.gestaobilhares.data.di.DatabaseModule;
import com.example.gestaobilhares.data.di.RepositoryModule;
import com.example.gestaobilhares.sync.di.SyncModule;
import com.example.gestaobilhares.ui.auth.AuthViewModel_HiltModules;
import com.example.gestaobilhares.ui.auth.ChangePasswordFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.auth.LoginFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.clients.ClientDetailFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.clients.ClientDetailViewModel_HiltModules;
import com.example.gestaobilhares.ui.clients.ClientListFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.clients.ClientListViewModel_HiltModules;
import com.example.gestaobilhares.ui.clients.ClientRegisterFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.clients.ClientRegisterViewModel_HiltModules;
import com.example.gestaobilhares.ui.clients.CycleHistoryFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.clients.CycleHistoryViewModel_HiltModules;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorManagementFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorManagementViewModel_HiltModules;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorMetasFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorRegisterFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.contracts.AditivoSignatureFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.contracts.AditivoSignatureViewModel_HiltModules;
import com.example.gestaobilhares.ui.contracts.ContractGenerationFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.contracts.ContractGenerationViewModel_HiltModules;
import com.example.gestaobilhares.ui.contracts.ContractManagementFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.contracts.ContractManagementViewModel_HiltModules;
import com.example.gestaobilhares.ui.contracts.RepresentanteLegalSignatureFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.contracts.RepresentanteLegalSignatureViewModel_HiltModules;
import com.example.gestaobilhares.ui.contracts.SignatureCaptureFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.contracts.SignatureCaptureViewModel_HiltModules;
import com.example.gestaobilhares.ui.cycles.CycleClientsFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.cycles.CycleClientsViewModel_HiltModules;
import com.example.gestaobilhares.ui.cycles.CycleExpensesFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.cycles.CycleExpensesViewModel_HiltModules;
import com.example.gestaobilhares.ui.cycles.CycleManagementFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.cycles.CycleManagementViewModel_HiltModules;
import com.example.gestaobilhares.ui.cycles.CycleReceiptsFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.cycles.CycleReceiptsViewModel_HiltModules;
import com.example.gestaobilhares.ui.dashboard.DashboardFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.dashboard.DashboardViewModel_HiltModules;
import com.example.gestaobilhares.ui.expenses.ExpenseCategoriesFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.expenses.ExpenseHistoryFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.expenses.ExpenseHistoryViewModel_HiltModules;
import com.example.gestaobilhares.ui.expenses.ExpenseRegisterFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.expenses.ExpenseRegisterViewModel_HiltModules;
import com.example.gestaobilhares.ui.expenses.ExpenseTypesFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.expenses.GlobalExpensesFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.expenses.GlobalExpensesViewModel_HiltModules;
import com.example.gestaobilhares.ui.inventory.equipments.AddEditEquipmentDialog_GeneratedInjector;
import com.example.gestaobilhares.ui.inventory.equipments.EquipmentsFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.inventory.equipments.EquipmentsViewModel_HiltModules;
import com.example.gestaobilhares.ui.inventory.stock.AddEditStockItemDialog_GeneratedInjector;
import com.example.gestaobilhares.ui.inventory.stock.AddPanosLoteDialog_GeneratedInjector;
import com.example.gestaobilhares.ui.inventory.stock.StockFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.inventory.stock.StockViewModel_HiltModules;
import com.example.gestaobilhares.ui.inventory.vehicles.AddEditFuelDialog_GeneratedInjector;
import com.example.gestaobilhares.ui.inventory.vehicles.AddEditVehicleDialog_GeneratedInjector;
import com.example.gestaobilhares.ui.inventory.vehicles.VehicleDetailFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.inventory.vehicles.VehicleDetailViewModel_HiltModules;
import com.example.gestaobilhares.ui.inventory.vehicles.VehiclesFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.inventory.vehicles.VehiclesViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.CadastroMesaFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.mesas.CadastroMesaViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.EditMesaFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.mesas.EditMesaViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.GerenciarMesasFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.mesas.GerenciarMesasViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.HistoricoManutencaoMesaFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.mesas.HistoricoManutencaoMesaViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.HistoricoMesasVendidasFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.mesas.HistoricoMesasVendidasViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.MesasDepositoFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.mesas.MesasDepositoViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.MesasReformadasFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.mesas.MesasReformadasViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.NovaReformaFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.mesas.NovaReformaViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.RotaMesasFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.mesas.RotaMesasViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.VendaMesaDialog_GeneratedInjector;
import com.example.gestaobilhares.ui.metas.MetaCadastroFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.metas.MetaCadastroViewModel_HiltModules;
import com.example.gestaobilhares.ui.metas.MetaHistoricoFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.metas.MetasFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.metas.MetasViewModel_HiltModules;
import com.example.gestaobilhares.ui.reports.ClosureReportFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.reports.ClosureReportViewModel_HiltModules;
import com.example.gestaobilhares.ui.routes.ClientSelectionDialog_GeneratedInjector;
import com.example.gestaobilhares.ui.routes.ClientSelectionViewModel_HiltModules;
import com.example.gestaobilhares.ui.routes.RoutesFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.routes.RoutesViewModel_HiltModules;
import com.example.gestaobilhares.ui.routes.TransferClientDialog_GeneratedInjector;
import com.example.gestaobilhares.ui.routes.TransferClientViewModel_HiltModules;
import com.example.gestaobilhares.ui.routes.management.RouteManagementFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.routes.management.RouteManagementViewModel_HiltModules;
import com.example.gestaobilhares.ui.settings.BackupViewModel_HiltModules;
import com.example.gestaobilhares.ui.settings.SettingsFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.settlement.PanoSelectionDialog_GeneratedInjector;
import com.example.gestaobilhares.ui.settlement.SettlementDetailFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.settlement.SettlementDetailViewModel_HiltModules;
import com.example.gestaobilhares.ui.settlement.SettlementFragment_GeneratedInjector;
import com.example.gestaobilhares.ui.settlement.SettlementViewModel_HiltModules;
import dagger.Binds;
import dagger.Component;
import dagger.Module;
import dagger.Subcomponent;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.components.ActivityRetainedComponent;
import dagger.hilt.android.components.FragmentComponent;
import dagger.hilt.android.components.ServiceComponent;
import dagger.hilt.android.components.ViewComponent;
import dagger.hilt.android.components.ViewModelComponent;
import dagger.hilt.android.components.ViewWithFragmentComponent;
import dagger.hilt.android.flags.FragmentGetContextFix;
import dagger.hilt.android.flags.HiltWrapper_FragmentGetContextFix_FragmentGetContextFixModule;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_DefaultViewModelFactories_ActivityModule;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint;
import dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory_ViewModelModule;
import dagger.hilt.android.internal.managers.ActivityComponentManager;
import dagger.hilt.android.internal.managers.FragmentComponentManager;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint;
import dagger.hilt.android.internal.managers.HiltWrapper_ActivityRetainedComponentManager_LifecycleModule;
import dagger.hilt.android.internal.managers.HiltWrapper_SavedStateHandleModule;
import dagger.hilt.android.internal.managers.ServiceComponentManager;
import dagger.hilt.android.internal.managers.ViewComponentManager;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.HiltWrapper_ActivityModule;
import dagger.hilt.android.scopes.ActivityRetainedScoped;
import dagger.hilt.android.scopes.ActivityScoped;
import dagger.hilt.android.scopes.FragmentScoped;
import dagger.hilt.android.scopes.ServiceScoped;
import dagger.hilt.android.scopes.ViewModelScoped;
import dagger.hilt.android.scopes.ViewScoped;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.internal.GeneratedComponent;
import dagger.hilt.migration.DisableInstallInCheck;
import javax.annotation.processing.Generated;
import javax.inject.Singleton;

@Generated("dagger.hilt.processor.internal.root.RootProcessor")
public final class GestaoBilharesApplication_HiltComponents {
  private GestaoBilharesApplication_HiltComponents() {
  }

  @Module(
      subcomponents = ServiceC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ServiceCBuilderModule {
    @Binds
    ServiceComponentBuilder bind(ServiceC.Builder builder);
  }

  @Module(
      subcomponents = ActivityRetainedC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ActivityRetainedCBuilderModule {
    @Binds
    ActivityRetainedComponentBuilder bind(ActivityRetainedC.Builder builder);
  }

  @Module(
      subcomponents = ActivityC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ActivityCBuilderModule {
    @Binds
    ActivityComponentBuilder bind(ActivityC.Builder builder);
  }

  @Module(
      subcomponents = ViewModelC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewModelCBuilderModule {
    @Binds
    ViewModelComponentBuilder bind(ViewModelC.Builder builder);
  }

  @Module(
      subcomponents = ViewC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewCBuilderModule {
    @Binds
    ViewComponentBuilder bind(ViewC.Builder builder);
  }

  @Module(
      subcomponents = FragmentC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface FragmentCBuilderModule {
    @Binds
    FragmentComponentBuilder bind(FragmentC.Builder builder);
  }

  @Module(
      subcomponents = ViewWithFragmentC.class
  )
  @DisableInstallInCheck
  @Generated("dagger.hilt.processor.internal.root.RootProcessor")
  abstract interface ViewWithFragmentCBuilderModule {
    @Binds
    ViewWithFragmentComponentBuilder bind(ViewWithFragmentC.Builder builder);
  }

  @Component(
      modules = {
          ApplicationContextModule.class,
          CoreModule.class,
          DatabaseModule.class,
          ActivityRetainedCBuilderModule.class,
          ServiceCBuilderModule.class,
          HiltWrapper_FragmentGetContextFix_FragmentGetContextFixModule.class,
          RepositoryModule.class,
          SyncModule.class
      }
  )
  @Singleton
  public abstract static class SingletonC implements GestaoBilharesApplication_GeneratedInjector,
      FragmentGetContextFix.FragmentGetContextFixEntryPoint,
      HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedComponentBuilderEntryPoint,
      ServiceComponentManager.ServiceComponentBuilderEntryPoint,
      SingletonComponent,
      GeneratedComponent {
  }

  @Subcomponent
  @ServiceScoped
  public abstract static class ServiceC implements ServiceComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ServiceComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          AditivoSignatureViewModel_HiltModules.KeyModule.class,
          AuthViewModel_HiltModules.KeyModule.class,
          BackupViewModel_HiltModules.KeyModule.class,
          CadastroMesaViewModel_HiltModules.KeyModule.class,
          ClientDetailViewModel_HiltModules.KeyModule.class,
          ClientListViewModel_HiltModules.KeyModule.class,
          ClientRegisterViewModel_HiltModules.KeyModule.class,
          ClientSelectionViewModel_HiltModules.KeyModule.class,
          ClosureReportViewModel_HiltModules.KeyModule.class,
          ColaboradorManagementViewModel_HiltModules.KeyModule.class,
          ContractGenerationViewModel_HiltModules.KeyModule.class,
          ContractManagementViewModel_HiltModules.KeyModule.class,
          CycleClientsViewModel_HiltModules.KeyModule.class,
          CycleExpensesViewModel_HiltModules.KeyModule.class,
          CycleHistoryViewModel_HiltModules.KeyModule.class,
          CycleManagementViewModel_HiltModules.KeyModule.class,
          CycleReceiptsViewModel_HiltModules.KeyModule.class,
          DashboardViewModel_HiltModules.KeyModule.class,
          EditMesaViewModel_HiltModules.KeyModule.class,
          EquipmentsViewModel_HiltModules.KeyModule.class,
          ExpenseHistoryViewModel_HiltModules.KeyModule.class,
          ExpenseRegisterViewModel_HiltModules.KeyModule.class,
          GerenciarMesasViewModel_HiltModules.KeyModule.class,
          ActivityCBuilderModule.class,
          ViewModelCBuilderModule.class,
          GlobalExpensesViewModel_HiltModules.KeyModule.class,
          HiltWrapper_ActivityRetainedComponentManager_LifecycleModule.class,
          HiltWrapper_SavedStateHandleModule.class,
          HistoricoManutencaoMesaViewModel_HiltModules.KeyModule.class,
          HistoricoMesasVendidasViewModel_HiltModules.KeyModule.class,
          MesasDepositoViewModel_HiltModules.KeyModule.class,
          MesasReformadasViewModel_HiltModules.KeyModule.class,
          MetaCadastroViewModel_HiltModules.KeyModule.class,
          MetasViewModel_HiltModules.KeyModule.class,
          NovaReformaViewModel_HiltModules.KeyModule.class,
          RepresentanteLegalSignatureViewModel_HiltModules.KeyModule.class,
          RotaMesasViewModel_HiltModules.KeyModule.class,
          RouteManagementViewModel_HiltModules.KeyModule.class,
          RoutesViewModel_HiltModules.KeyModule.class,
          SettlementDetailViewModel_HiltModules.KeyModule.class,
          SettlementViewModel_HiltModules.KeyModule.class,
          SignatureCaptureViewModel_HiltModules.KeyModule.class,
          StockViewModel_HiltModules.KeyModule.class,
          TransferClientViewModel_HiltModules.KeyModule.class,
          VehicleDetailViewModel_HiltModules.KeyModule.class,
          VehiclesViewModel_HiltModules.KeyModule.class
      }
  )
  @ActivityRetainedScoped
  public abstract static class ActivityRetainedC implements ActivityRetainedComponent,
      ActivityComponentManager.ActivityComponentBuilderEntryPoint,
      HiltWrapper_ActivityRetainedComponentManager_ActivityRetainedLifecycleEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ActivityRetainedComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          FragmentCBuilderModule.class,
          ViewCBuilderModule.class,
          HiltWrapper_ActivityModule.class,
          HiltWrapper_DefaultViewModelFactories_ActivityModule.class
      }
  )
  @ActivityScoped
  public abstract static class ActivityC implements MainActivity_GeneratedInjector,
      ActivityComponent,
      DefaultViewModelFactories.ActivityEntryPoint,
      HiltWrapper_HiltViewModelFactory_ActivityCreatorEntryPoint,
      FragmentComponentManager.FragmentComponentBuilderEntryPoint,
      ViewComponentManager.ViewComponentBuilderEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ActivityComponentBuilder {
    }
  }

  @Subcomponent(
      modules = {
          AditivoSignatureViewModel_HiltModules.BindsModule.class,
          AuthViewModel_HiltModules.BindsModule.class,
          BackupViewModel_HiltModules.BindsModule.class,
          CadastroMesaViewModel_HiltModules.BindsModule.class,
          ClientDetailViewModel_HiltModules.BindsModule.class,
          ClientListViewModel_HiltModules.BindsModule.class,
          ClientRegisterViewModel_HiltModules.BindsModule.class,
          ClientSelectionViewModel_HiltModules.BindsModule.class,
          ClosureReportViewModel_HiltModules.BindsModule.class,
          ColaboradorManagementViewModel_HiltModules.BindsModule.class,
          ContractGenerationViewModel_HiltModules.BindsModule.class,
          ContractManagementViewModel_HiltModules.BindsModule.class,
          CycleClientsViewModel_HiltModules.BindsModule.class,
          CycleExpensesViewModel_HiltModules.BindsModule.class,
          CycleHistoryViewModel_HiltModules.BindsModule.class,
          CycleManagementViewModel_HiltModules.BindsModule.class,
          CycleReceiptsViewModel_HiltModules.BindsModule.class,
          DashboardViewModel_HiltModules.BindsModule.class,
          EditMesaViewModel_HiltModules.BindsModule.class,
          EquipmentsViewModel_HiltModules.BindsModule.class,
          ExpenseHistoryViewModel_HiltModules.BindsModule.class,
          ExpenseRegisterViewModel_HiltModules.BindsModule.class,
          GerenciarMesasViewModel_HiltModules.BindsModule.class,
          GlobalExpensesViewModel_HiltModules.BindsModule.class,
          HiltWrapper_HiltViewModelFactory_ViewModelModule.class,
          HistoricoManutencaoMesaViewModel_HiltModules.BindsModule.class,
          HistoricoMesasVendidasViewModel_HiltModules.BindsModule.class,
          MesasDepositoViewModel_HiltModules.BindsModule.class,
          MesasReformadasViewModel_HiltModules.BindsModule.class,
          MetaCadastroViewModel_HiltModules.BindsModule.class,
          MetasViewModel_HiltModules.BindsModule.class,
          NovaReformaViewModel_HiltModules.BindsModule.class,
          RepresentanteLegalSignatureViewModel_HiltModules.BindsModule.class,
          RotaMesasViewModel_HiltModules.BindsModule.class,
          RouteManagementViewModel_HiltModules.BindsModule.class,
          RoutesViewModel_HiltModules.BindsModule.class,
          SettlementDetailViewModel_HiltModules.BindsModule.class,
          SettlementViewModel_HiltModules.BindsModule.class,
          SignatureCaptureViewModel_HiltModules.BindsModule.class,
          StockViewModel_HiltModules.BindsModule.class,
          TransferClientViewModel_HiltModules.BindsModule.class,
          VehicleDetailViewModel_HiltModules.BindsModule.class,
          VehiclesViewModel_HiltModules.BindsModule.class
      }
  )
  @ViewModelScoped
  public abstract static class ViewModelC implements ViewModelComponent,
      HiltViewModelFactory.ViewModelFactoriesEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewModelComponentBuilder {
    }
  }

  @Subcomponent
  @ViewScoped
  public abstract static class ViewC implements ViewComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewComponentBuilder {
    }
  }

  @Subcomponent(
      modules = ViewWithFragmentCBuilderModule.class
  )
  @FragmentScoped
  public abstract static class FragmentC implements ChangePasswordFragment_GeneratedInjector,
      LoginFragment_GeneratedInjector,
      ClientDetailFragment_GeneratedInjector,
      ClientListFragment_GeneratedInjector,
      ClientRegisterFragment_GeneratedInjector,
      CycleHistoryFragment_GeneratedInjector,
      ColaboradorManagementFragment_GeneratedInjector,
      ColaboradorMetasFragment_GeneratedInjector,
      ColaboradorRegisterFragment_GeneratedInjector,
      AditivoSignatureFragment_GeneratedInjector,
      ContractGenerationFragment_GeneratedInjector,
      ContractManagementFragment_GeneratedInjector,
      RepresentanteLegalSignatureFragment_GeneratedInjector,
      SignatureCaptureFragment_GeneratedInjector,
      CycleClientsFragment_GeneratedInjector,
      CycleExpensesFragment_GeneratedInjector,
      CycleManagementFragment_GeneratedInjector,
      CycleReceiptsFragment_GeneratedInjector,
      DashboardFragment_GeneratedInjector,
      ExpenseCategoriesFragment_GeneratedInjector,
      ExpenseHistoryFragment_GeneratedInjector,
      ExpenseRegisterFragment_GeneratedInjector,
      ExpenseTypesFragment_GeneratedInjector,
      GlobalExpensesFragment_GeneratedInjector,
      AddEditEquipmentDialog_GeneratedInjector,
      EquipmentsFragment_GeneratedInjector,
      AddEditStockItemDialog_GeneratedInjector,
      AddPanosLoteDialog_GeneratedInjector,
      StockFragment_GeneratedInjector,
      AddEditFuelDialog_GeneratedInjector,
      AddEditVehicleDialog_GeneratedInjector,
      VehicleDetailFragment_GeneratedInjector,
      VehiclesFragment_GeneratedInjector,
      CadastroMesaFragment_GeneratedInjector,
      EditMesaFragment_GeneratedInjector,
      GerenciarMesasFragment_GeneratedInjector,
      HistoricoManutencaoMesaFragment_GeneratedInjector,
      HistoricoMesasVendidasFragment_GeneratedInjector,
      MesasDepositoFragment_GeneratedInjector,
      MesasReformadasFragment_GeneratedInjector,
      NovaReformaFragment_GeneratedInjector,
      RotaMesasFragment_GeneratedInjector,
      VendaMesaDialog_GeneratedInjector,
      MetaCadastroFragment_GeneratedInjector,
      MetaHistoricoFragment_GeneratedInjector,
      MetasFragment_GeneratedInjector,
      ClosureReportFragment_GeneratedInjector,
      ClientSelectionDialog_GeneratedInjector,
      RoutesFragment_GeneratedInjector,
      TransferClientDialog_GeneratedInjector,
      RouteManagementFragment_GeneratedInjector,
      SettingsFragment_GeneratedInjector,
      PanoSelectionDialog_GeneratedInjector,
      SettlementDetailFragment_GeneratedInjector,
      SettlementFragment_GeneratedInjector,
      FragmentComponent,
      DefaultViewModelFactories.FragmentEntryPoint,
      ViewComponentManager.ViewWithFragmentComponentBuilderEntryPoint,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends FragmentComponentBuilder {
    }
  }

  @Subcomponent
  @ViewScoped
  public abstract static class ViewWithFragmentC implements ViewWithFragmentComponent,
      GeneratedComponent {
    @Subcomponent.Builder
    abstract interface Builder extends ViewWithFragmentComponentBuilder {
    }
  }
}
