package com.example.gestaobilhares;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.gestaobilhares.core.di.CoreModule_ProvideNetworkUtilsFactory;
import com.example.gestaobilhares.core.di.CoreModule_ProvideUserSessionManagerFactory;
import com.example.gestaobilhares.core.utils.NetworkUtils;
import com.example.gestaobilhares.core.utils.UserSessionManager;
import com.example.gestaobilhares.data.dao.AcertoDao;
import com.example.gestaobilhares.data.dao.CicloAcertoDao;
import com.example.gestaobilhares.data.dao.ClienteDao;
import com.example.gestaobilhares.data.dao.ColaboradorDao;
import com.example.gestaobilhares.data.dao.DespesaDao;
import com.example.gestaobilhares.data.dao.RotaDao;
import com.example.gestaobilhares.data.database.AppDatabase;
import com.example.gestaobilhares.data.di.DatabaseModule_ProvideAcertoDaoFactory;
import com.example.gestaobilhares.data.di.DatabaseModule_ProvideAppDatabaseFactory;
import com.example.gestaobilhares.data.di.DatabaseModule_ProvideCicloDaoFactory;
import com.example.gestaobilhares.data.di.DatabaseModule_ProvideClienteDaoFactory;
import com.example.gestaobilhares.data.di.DatabaseModule_ProvideColaboradorDaoFactory;
import com.example.gestaobilhares.data.di.DatabaseModule_ProvideDespesaDaoFactory;
import com.example.gestaobilhares.data.di.DatabaseModule_ProvideRotaDaoFactory;
import com.example.gestaobilhares.data.di.RepositoryModule_ProvideAcertoRepositoryFactory;
import com.example.gestaobilhares.data.di.RepositoryModule_ProvideAppRepositoryFactory;
import com.example.gestaobilhares.data.di.RepositoryModule_ProvideCicloAcertoRepositoryFactory;
import com.example.gestaobilhares.data.di.RepositoryModule_ProvideClienteRepositoryFactory;
import com.example.gestaobilhares.data.di.RepositoryModule_ProvideFirebaseFirestoreFactory;
import com.example.gestaobilhares.data.repository.AcertoRepository;
import com.example.gestaobilhares.data.repository.AppRepository;
import com.example.gestaobilhares.data.repository.CicloAcertoRepository;
import com.example.gestaobilhares.data.repository.ClienteRepository;
import com.example.gestaobilhares.sync.SyncRepository;
import com.example.gestaobilhares.sync.di.SyncModule_ProvideNetworkUtilsFactory;
import com.example.gestaobilhares.sync.di.SyncModule_ProvideSyncRepositoryFactory;
import com.example.gestaobilhares.ui.auth.AuthViewModel;
import com.example.gestaobilhares.ui.auth.AuthViewModel_HiltModules;
import com.example.gestaobilhares.ui.auth.ChangePasswordFragment;
import com.example.gestaobilhares.ui.auth.LoginFragment;
import com.example.gestaobilhares.ui.clients.ClientDetailFragment;
import com.example.gestaobilhares.ui.clients.ClientDetailFragment_MembersInjector;
import com.example.gestaobilhares.ui.clients.ClientDetailViewModel;
import com.example.gestaobilhares.ui.clients.ClientDetailViewModel_HiltModules;
import com.example.gestaobilhares.ui.clients.ClientListFragment;
import com.example.gestaobilhares.ui.clients.ClientListFragment_MembersInjector;
import com.example.gestaobilhares.ui.clients.ClientListViewModel;
import com.example.gestaobilhares.ui.clients.ClientListViewModel_HiltModules;
import com.example.gestaobilhares.ui.clients.ClientRegisterFragment;
import com.example.gestaobilhares.ui.clients.ClientRegisterViewModel;
import com.example.gestaobilhares.ui.clients.ClientRegisterViewModel_HiltModules;
import com.example.gestaobilhares.ui.clients.CycleHistoryFragment;
import com.example.gestaobilhares.ui.clients.CycleHistoryViewModel;
import com.example.gestaobilhares.ui.clients.CycleHistoryViewModel_HiltModules;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorManagementFragment;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorManagementViewModel;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorManagementViewModel_HiltModules;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorMetasFragment;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorMetasFragment_MembersInjector;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorRegisterFragment;
import com.example.gestaobilhares.ui.colaboradores.ColaboradorRegisterFragment_MembersInjector;
import com.example.gestaobilhares.ui.contracts.AditivoSignatureFragment;
import com.example.gestaobilhares.ui.contracts.AditivoSignatureFragment_MembersInjector;
import com.example.gestaobilhares.ui.contracts.AditivoSignatureViewModel;
import com.example.gestaobilhares.ui.contracts.AditivoSignatureViewModel_HiltModules;
import com.example.gestaobilhares.ui.contracts.ContractGenerationFragment;
import com.example.gestaobilhares.ui.contracts.ContractGenerationFragment_MembersInjector;
import com.example.gestaobilhares.ui.contracts.ContractGenerationViewModel;
import com.example.gestaobilhares.ui.contracts.ContractGenerationViewModel_HiltModules;
import com.example.gestaobilhares.ui.contracts.ContractManagementFragment;
import com.example.gestaobilhares.ui.contracts.ContractManagementFragment_MembersInjector;
import com.example.gestaobilhares.ui.contracts.ContractManagementViewModel;
import com.example.gestaobilhares.ui.contracts.ContractManagementViewModel_HiltModules;
import com.example.gestaobilhares.ui.contracts.RepresentanteLegalSignatureFragment;
import com.example.gestaobilhares.ui.contracts.RepresentanteLegalSignatureViewModel;
import com.example.gestaobilhares.ui.contracts.RepresentanteLegalSignatureViewModel_HiltModules;
import com.example.gestaobilhares.ui.contracts.SignatureCaptureFragment;
import com.example.gestaobilhares.ui.contracts.SignatureCaptureFragment_MembersInjector;
import com.example.gestaobilhares.ui.contracts.SignatureCaptureViewModel;
import com.example.gestaobilhares.ui.contracts.SignatureCaptureViewModel_HiltModules;
import com.example.gestaobilhares.ui.cycles.CycleClientsFragment;
import com.example.gestaobilhares.ui.cycles.CycleClientsViewModel;
import com.example.gestaobilhares.ui.cycles.CycleClientsViewModel_HiltModules;
import com.example.gestaobilhares.ui.cycles.CycleExpensesFragment;
import com.example.gestaobilhares.ui.cycles.CycleExpensesViewModel;
import com.example.gestaobilhares.ui.cycles.CycleExpensesViewModel_HiltModules;
import com.example.gestaobilhares.ui.cycles.CycleManagementFragment;
import com.example.gestaobilhares.ui.cycles.CycleManagementViewModel;
import com.example.gestaobilhares.ui.cycles.CycleManagementViewModel_HiltModules;
import com.example.gestaobilhares.ui.cycles.CycleReceiptsFragment;
import com.example.gestaobilhares.ui.cycles.CycleReceiptsViewModel;
import com.example.gestaobilhares.ui.cycles.CycleReceiptsViewModel_HiltModules;
import com.example.gestaobilhares.ui.dashboard.DashboardFragment;
import com.example.gestaobilhares.ui.dashboard.DashboardViewModel;
import com.example.gestaobilhares.ui.dashboard.DashboardViewModel_HiltModules;
import com.example.gestaobilhares.ui.expenses.ExpenseCategoriesFragment;
import com.example.gestaobilhares.ui.expenses.ExpenseCategoriesFragment_MembersInjector;
import com.example.gestaobilhares.ui.expenses.ExpenseHistoryFragment;
import com.example.gestaobilhares.ui.expenses.ExpenseHistoryViewModel;
import com.example.gestaobilhares.ui.expenses.ExpenseHistoryViewModel_HiltModules;
import com.example.gestaobilhares.ui.expenses.ExpenseRegisterFragment;
import com.example.gestaobilhares.ui.expenses.ExpenseRegisterViewModel;
import com.example.gestaobilhares.ui.expenses.ExpenseRegisterViewModel_HiltModules;
import com.example.gestaobilhares.ui.expenses.ExpenseTypesFragment;
import com.example.gestaobilhares.ui.expenses.ExpenseTypesFragment_MembersInjector;
import com.example.gestaobilhares.ui.expenses.GlobalExpensesFragment;
import com.example.gestaobilhares.ui.expenses.GlobalExpensesViewModel;
import com.example.gestaobilhares.ui.expenses.GlobalExpensesViewModel_HiltModules;
import com.example.gestaobilhares.ui.inventory.equipments.AddEditEquipmentDialog;
import com.example.gestaobilhares.ui.inventory.equipments.EquipmentsFragment;
import com.example.gestaobilhares.ui.inventory.equipments.EquipmentsViewModel;
import com.example.gestaobilhares.ui.inventory.equipments.EquipmentsViewModel_HiltModules;
import com.example.gestaobilhares.ui.inventory.stock.AddEditStockItemDialog;
import com.example.gestaobilhares.ui.inventory.stock.AddPanosLoteDialog;
import com.example.gestaobilhares.ui.inventory.stock.StockFragment;
import com.example.gestaobilhares.ui.inventory.stock.StockViewModel;
import com.example.gestaobilhares.ui.inventory.stock.StockViewModel_HiltModules;
import com.example.gestaobilhares.ui.inventory.vehicles.AddEditFuelDialog;
import com.example.gestaobilhares.ui.inventory.vehicles.AddEditFuelDialog_MembersInjector;
import com.example.gestaobilhares.ui.inventory.vehicles.AddEditVehicleDialog;
import com.example.gestaobilhares.ui.inventory.vehicles.VehicleDetailFragment;
import com.example.gestaobilhares.ui.inventory.vehicles.VehicleDetailViewModel;
import com.example.gestaobilhares.ui.inventory.vehicles.VehicleDetailViewModel_HiltModules;
import com.example.gestaobilhares.ui.inventory.vehicles.VehiclesFragment;
import com.example.gestaobilhares.ui.inventory.vehicles.VehiclesViewModel;
import com.example.gestaobilhares.ui.inventory.vehicles.VehiclesViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.CadastroMesaFragment;
import com.example.gestaobilhares.ui.mesas.CadastroMesaViewModel;
import com.example.gestaobilhares.ui.mesas.CadastroMesaViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.EditMesaFragment;
import com.example.gestaobilhares.ui.mesas.EditMesaViewModel;
import com.example.gestaobilhares.ui.mesas.EditMesaViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.GerenciarMesasFragment;
import com.example.gestaobilhares.ui.mesas.GerenciarMesasViewModel;
import com.example.gestaobilhares.ui.mesas.GerenciarMesasViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.HistoricoManutencaoMesaFragment;
import com.example.gestaobilhares.ui.mesas.HistoricoManutencaoMesaViewModel;
import com.example.gestaobilhares.ui.mesas.HistoricoManutencaoMesaViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.HistoricoMesasVendidasFragment;
import com.example.gestaobilhares.ui.mesas.HistoricoMesasVendidasViewModel;
import com.example.gestaobilhares.ui.mesas.HistoricoMesasVendidasViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.MesasDepositoFragment;
import com.example.gestaobilhares.ui.mesas.MesasDepositoFragment_MembersInjector;
import com.example.gestaobilhares.ui.mesas.MesasDepositoViewModel;
import com.example.gestaobilhares.ui.mesas.MesasDepositoViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.MesasReformadasFragment;
import com.example.gestaobilhares.ui.mesas.MesasReformadasViewModel;
import com.example.gestaobilhares.ui.mesas.MesasReformadasViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.NovaReformaFragment;
import com.example.gestaobilhares.ui.mesas.NovaReformaViewModel;
import com.example.gestaobilhares.ui.mesas.NovaReformaViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.RotaMesasFragment;
import com.example.gestaobilhares.ui.mesas.RotaMesasViewModel;
import com.example.gestaobilhares.ui.mesas.RotaMesasViewModel_HiltModules;
import com.example.gestaobilhares.ui.mesas.VendaMesaDialog;
import com.example.gestaobilhares.ui.mesas.VendaMesaDialog_MembersInjector;
import com.example.gestaobilhares.ui.metas.MetaCadastroFragment;
import com.example.gestaobilhares.ui.metas.MetaCadastroViewModel;
import com.example.gestaobilhares.ui.metas.MetaCadastroViewModel_HiltModules;
import com.example.gestaobilhares.ui.metas.MetaHistoricoFragment;
import com.example.gestaobilhares.ui.metas.MetaHistoricoFragment_MembersInjector;
import com.example.gestaobilhares.ui.metas.MetasFragment;
import com.example.gestaobilhares.ui.metas.MetasViewModel;
import com.example.gestaobilhares.ui.metas.MetasViewModel_HiltModules;
import com.example.gestaobilhares.ui.reports.ClosureReportFragment;
import com.example.gestaobilhares.ui.reports.ClosureReportViewModel;
import com.example.gestaobilhares.ui.reports.ClosureReportViewModel_HiltModules;
import com.example.gestaobilhares.ui.routes.ClientSelectionDialog;
import com.example.gestaobilhares.ui.routes.ClientSelectionViewModel;
import com.example.gestaobilhares.ui.routes.ClientSelectionViewModel_HiltModules;
import com.example.gestaobilhares.ui.routes.RoutesFragment;
import com.example.gestaobilhares.ui.routes.RoutesViewModel;
import com.example.gestaobilhares.ui.routes.RoutesViewModel_HiltModules;
import com.example.gestaobilhares.ui.routes.TransferClientDialog;
import com.example.gestaobilhares.ui.routes.TransferClientViewModel;
import com.example.gestaobilhares.ui.routes.TransferClientViewModel_HiltModules;
import com.example.gestaobilhares.ui.routes.management.RouteManagementFragment;
import com.example.gestaobilhares.ui.routes.management.RouteManagementViewModel;
import com.example.gestaobilhares.ui.routes.management.RouteManagementViewModel_HiltModules;
import com.example.gestaobilhares.ui.settlement.PanoSelectionDialog;
import com.example.gestaobilhares.ui.settlement.PanoSelectionDialog_MembersInjector;
import com.example.gestaobilhares.ui.settlement.SettlementDetailFragment;
import com.example.gestaobilhares.ui.settlement.SettlementDetailFragment_MembersInjector;
import com.example.gestaobilhares.ui.settlement.SettlementDetailViewModel;
import com.example.gestaobilhares.ui.settlement.SettlementDetailViewModel_HiltModules;
import com.example.gestaobilhares.ui.settlement.SettlementFragment;
import com.example.gestaobilhares.ui.settlement.SettlementFragment_MembersInjector;
import com.example.gestaobilhares.ui.settlement.SettlementViewModel;
import com.example.gestaobilhares.ui.settlement.SettlementViewModel_HiltModules;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.firebase.firestore.FirebaseFirestore;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DaggerGestaoBilharesApplication_HiltComponents_SingletonC {
  private DaggerGestaoBilharesApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public GestaoBilharesApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements GestaoBilharesApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public GestaoBilharesApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements GestaoBilharesApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public GestaoBilharesApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements GestaoBilharesApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public GestaoBilharesApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements GestaoBilharesApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public GestaoBilharesApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements GestaoBilharesApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public GestaoBilharesApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements GestaoBilharesApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public GestaoBilharesApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements GestaoBilharesApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public GestaoBilharesApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends GestaoBilharesApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends GestaoBilharesApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public void injectChangePasswordFragment(ChangePasswordFragment arg0) {
    }

    @Override
    public void injectLoginFragment(LoginFragment arg0) {
    }

    @Override
    public void injectClientDetailFragment(ClientDetailFragment arg0) {
      injectClientDetailFragment2(arg0);
    }

    @Override
    public void injectClientListFragment(ClientListFragment arg0) {
      injectClientListFragment2(arg0);
    }

    @Override
    public void injectClientRegisterFragment(ClientRegisterFragment arg0) {
    }

    @Override
    public void injectCycleHistoryFragment(CycleHistoryFragment arg0) {
    }

    @Override
    public void injectColaboradorManagementFragment(ColaboradorManagementFragment arg0) {
    }

    @Override
    public void injectColaboradorMetasFragment(ColaboradorMetasFragment arg0) {
      injectColaboradorMetasFragment2(arg0);
    }

    @Override
    public void injectColaboradorRegisterFragment(ColaboradorRegisterFragment arg0) {
      injectColaboradorRegisterFragment2(arg0);
    }

    @Override
    public void injectAditivoSignatureFragment(AditivoSignatureFragment arg0) {
      injectAditivoSignatureFragment2(arg0);
    }

    @Override
    public void injectContractGenerationFragment(ContractGenerationFragment arg0) {
      injectContractGenerationFragment2(arg0);
    }

    @Override
    public void injectContractManagementFragment(ContractManagementFragment arg0) {
      injectContractManagementFragment2(arg0);
    }

    @Override
    public void injectRepresentanteLegalSignatureFragment(
        RepresentanteLegalSignatureFragment arg0) {
    }

    @Override
    public void injectSignatureCaptureFragment(SignatureCaptureFragment arg0) {
      injectSignatureCaptureFragment2(arg0);
    }

    @Override
    public void injectCycleClientsFragment(CycleClientsFragment arg0) {
    }

    @Override
    public void injectCycleExpensesFragment(CycleExpensesFragment arg0) {
    }

    @Override
    public void injectCycleManagementFragment(CycleManagementFragment arg0) {
    }

    @Override
    public void injectCycleReceiptsFragment(CycleReceiptsFragment arg0) {
    }

    @Override
    public void injectDashboardFragment(DashboardFragment arg0) {
    }

    @Override
    public void injectExpenseCategoriesFragment(ExpenseCategoriesFragment arg0) {
      injectExpenseCategoriesFragment2(arg0);
    }

    @Override
    public void injectExpenseHistoryFragment(ExpenseHistoryFragment arg0) {
    }

    @Override
    public void injectExpenseRegisterFragment(ExpenseRegisterFragment arg0) {
    }

    @Override
    public void injectExpenseTypesFragment(ExpenseTypesFragment arg0) {
      injectExpenseTypesFragment2(arg0);
    }

    @Override
    public void injectGlobalExpensesFragment(GlobalExpensesFragment arg0) {
    }

    @Override
    public void injectAddEditEquipmentDialog(AddEditEquipmentDialog arg0) {
    }

    @Override
    public void injectEquipmentsFragment(EquipmentsFragment arg0) {
    }

    @Override
    public void injectAddEditStockItemDialog(AddEditStockItemDialog arg0) {
    }

    @Override
    public void injectAddPanosLoteDialog(AddPanosLoteDialog arg0) {
    }

    @Override
    public void injectStockFragment(StockFragment arg0) {
    }

    @Override
    public void injectAddEditFuelDialog(AddEditFuelDialog arg0) {
      injectAddEditFuelDialog2(arg0);
    }

    @Override
    public void injectAddEditVehicleDialog(AddEditVehicleDialog arg0) {
    }

    @Override
    public void injectVehicleDetailFragment(VehicleDetailFragment arg0) {
    }

    @Override
    public void injectVehiclesFragment(VehiclesFragment arg0) {
    }

    @Override
    public void injectCadastroMesaFragment(CadastroMesaFragment arg0) {
    }

    @Override
    public void injectEditMesaFragment(EditMesaFragment arg0) {
    }

    @Override
    public void injectGerenciarMesasFragment(GerenciarMesasFragment arg0) {
    }

    @Override
    public void injectHistoricoManutencaoMesaFragment(HistoricoManutencaoMesaFragment arg0) {
    }

    @Override
    public void injectHistoricoMesasVendidasFragment(HistoricoMesasVendidasFragment arg0) {
    }

    @Override
    public void injectMesasDepositoFragment(MesasDepositoFragment arg0) {
      injectMesasDepositoFragment2(arg0);
    }

    @Override
    public void injectMesasReformadasFragment(MesasReformadasFragment arg0) {
    }

    @Override
    public void injectNovaReformaFragment(NovaReformaFragment arg0) {
    }

    @Override
    public void injectRotaMesasFragment(RotaMesasFragment arg0) {
    }

    @Override
    public void injectVendaMesaDialog(VendaMesaDialog arg0) {
      injectVendaMesaDialog2(arg0);
    }

    @Override
    public void injectMetaCadastroFragment(MetaCadastroFragment arg0) {
    }

    @Override
    public void injectMetaHistoricoFragment(MetaHistoricoFragment arg0) {
      injectMetaHistoricoFragment2(arg0);
    }

    @Override
    public void injectMetasFragment(MetasFragment arg0) {
    }

    @Override
    public void injectClosureReportFragment(ClosureReportFragment arg0) {
    }

    @Override
    public void injectClientSelectionDialog(ClientSelectionDialog arg0) {
    }

    @Override
    public void injectRoutesFragment(RoutesFragment arg0) {
    }

    @Override
    public void injectTransferClientDialog(TransferClientDialog arg0) {
    }

    @Override
    public void injectRouteManagementFragment(RouteManagementFragment arg0) {
    }

    @Override
    public void injectPanoSelectionDialog(PanoSelectionDialog arg0) {
      injectPanoSelectionDialog2(arg0);
    }

    @Override
    public void injectSettlementDetailFragment(SettlementDetailFragment arg0) {
      injectSettlementDetailFragment2(arg0);
    }

    @Override
    public void injectSettlementFragment(SettlementFragment arg0) {
      injectSettlementFragment2(arg0);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }

    @CanIgnoreReturnValue
    private ClientDetailFragment injectClientDetailFragment2(ClientDetailFragment instance) {
      ClientDetailFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      ClientDetailFragment_MembersInjector.injectSyncRepository(instance, singletonCImpl.provideSyncRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ClientListFragment injectClientListFragment2(ClientListFragment instance) {
      ClientListFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ColaboradorMetasFragment injectColaboradorMetasFragment2(
        ColaboradorMetasFragment instance) {
      ColaboradorMetasFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ColaboradorRegisterFragment injectColaboradorRegisterFragment2(
        ColaboradorRegisterFragment instance) {
      ColaboradorRegisterFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private AditivoSignatureFragment injectAditivoSignatureFragment2(
        AditivoSignatureFragment instance) {
      AditivoSignatureFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ContractGenerationFragment injectContractGenerationFragment2(
        ContractGenerationFragment instance) {
      ContractGenerationFragment_MembersInjector.injectRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ContractManagementFragment injectContractManagementFragment2(
        ContractManagementFragment instance) {
      ContractManagementFragment_MembersInjector.injectRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private SignatureCaptureFragment injectSignatureCaptureFragment2(
        SignatureCaptureFragment instance) {
      SignatureCaptureFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ExpenseCategoriesFragment injectExpenseCategoriesFragment2(
        ExpenseCategoriesFragment instance) {
      ExpenseCategoriesFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private ExpenseTypesFragment injectExpenseTypesFragment2(ExpenseTypesFragment instance) {
      ExpenseTypesFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private AddEditFuelDialog injectAddEditFuelDialog2(AddEditFuelDialog instance) {
      AddEditFuelDialog_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private MesasDepositoFragment injectMesasDepositoFragment2(MesasDepositoFragment instance) {
      MesasDepositoFragment_MembersInjector.injectUserSessionManager(instance, singletonCImpl.provideUserSessionManagerProvider.get());
      MesasDepositoFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private VendaMesaDialog injectVendaMesaDialog2(VendaMesaDialog instance) {
      VendaMesaDialog_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private MetaHistoricoFragment injectMetaHistoricoFragment2(MetaHistoricoFragment instance) {
      MetaHistoricoFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private PanoSelectionDialog injectPanoSelectionDialog2(PanoSelectionDialog instance) {
      PanoSelectionDialog_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private SettlementDetailFragment injectSettlementDetailFragment2(
        SettlementDetailFragment instance) {
      SettlementDetailFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }

    @CanIgnoreReturnValue
    private SettlementFragment injectSettlementFragment2(SettlementFragment instance) {
      SettlementFragment_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      return instance;
    }
  }

  private static final class ViewCImpl extends GestaoBilharesApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends GestaoBilharesApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(41).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_contracts_AditivoSignatureViewModel, AditivoSignatureViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_auth_AuthViewModel, AuthViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_CadastroMesaViewModel, CadastroMesaViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_clients_ClientDetailViewModel, ClientDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_clients_ClientListViewModel, ClientListViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_clients_ClientRegisterViewModel, ClientRegisterViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_routes_ClientSelectionViewModel, ClientSelectionViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_reports_ClosureReportViewModel, ClosureReportViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_colaboradores_ColaboradorManagementViewModel, ColaboradorManagementViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_contracts_ContractGenerationViewModel, ContractGenerationViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_contracts_ContractManagementViewModel, ContractManagementViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_cycles_CycleClientsViewModel, CycleClientsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_cycles_CycleExpensesViewModel, CycleExpensesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_clients_CycleHistoryViewModel, CycleHistoryViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_cycles_CycleManagementViewModel, CycleManagementViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_cycles_CycleReceiptsViewModel, CycleReceiptsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_dashboard_DashboardViewModel, DashboardViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_EditMesaViewModel, EditMesaViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_inventory_equipments_EquipmentsViewModel, EquipmentsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_expenses_ExpenseHistoryViewModel, ExpenseHistoryViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_expenses_ExpenseRegisterViewModel, ExpenseRegisterViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_GerenciarMesasViewModel, GerenciarMesasViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_expenses_GlobalExpensesViewModel, GlobalExpensesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_HistoricoManutencaoMesaViewModel, HistoricoManutencaoMesaViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_HistoricoMesasVendidasViewModel, HistoricoMesasVendidasViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_MesasDepositoViewModel, MesasDepositoViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_MesasReformadasViewModel, MesasReformadasViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_metas_MetaCadastroViewModel, MetaCadastroViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_metas_MetasViewModel, MetasViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_NovaReformaViewModel, NovaReformaViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_contracts_RepresentanteLegalSignatureViewModel, RepresentanteLegalSignatureViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_RotaMesasViewModel, RotaMesasViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_routes_management_RouteManagementViewModel, RouteManagementViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_routes_RoutesViewModel, RoutesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_settlement_SettlementDetailViewModel, SettlementDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_settlement_SettlementViewModel, SettlementViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_contracts_SignatureCaptureViewModel, SignatureCaptureViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_inventory_stock_StockViewModel, StockViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_routes_TransferClientViewModel, TransferClientViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_inventory_vehicles_VehicleDetailViewModel, VehicleDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_inventory_vehicles_VehiclesViewModel, VehiclesViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectAppRepository(instance, singletonCImpl.provideAppRepositoryProvider.get());
      MainActivity_MembersInjector.injectNetworkUtils(instance, singletonCImpl.provideNetworkUtilsProvider.get());
      return instance;
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_example_gestaobilhares_ui_routes_management_RouteManagementViewModel = "com.example.gestaobilhares.ui.routes.management.RouteManagementViewModel";

      static String com_example_gestaobilhares_ui_routes_TransferClientViewModel = "com.example.gestaobilhares.ui.routes.TransferClientViewModel";

      static String com_example_gestaobilhares_ui_expenses_ExpenseHistoryViewModel = "com.example.gestaobilhares.ui.expenses.ExpenseHistoryViewModel";

      static String com_example_gestaobilhares_ui_clients_ClientListViewModel = "com.example.gestaobilhares.ui.clients.ClientListViewModel";

      static String com_example_gestaobilhares_ui_contracts_ContractManagementViewModel = "com.example.gestaobilhares.ui.contracts.ContractManagementViewModel";

      static String com_example_gestaobilhares_ui_contracts_ContractGenerationViewModel = "com.example.gestaobilhares.ui.contracts.ContractGenerationViewModel";

      static String com_example_gestaobilhares_ui_settlement_SettlementDetailViewModel = "com.example.gestaobilhares.ui.settlement.SettlementDetailViewModel";

      static String com_example_gestaobilhares_ui_mesas_GerenciarMesasViewModel = "com.example.gestaobilhares.ui.mesas.GerenciarMesasViewModel";

      static String com_example_gestaobilhares_ui_clients_ClientRegisterViewModel = "com.example.gestaobilhares.ui.clients.ClientRegisterViewModel";

      static String com_example_gestaobilhares_ui_mesas_CadastroMesaViewModel = "com.example.gestaobilhares.ui.mesas.CadastroMesaViewModel";

      static String com_example_gestaobilhares_ui_inventory_equipments_EquipmentsViewModel = "com.example.gestaobilhares.ui.inventory.equipments.EquipmentsViewModel";

      static String com_example_gestaobilhares_ui_expenses_GlobalExpensesViewModel = "com.example.gestaobilhares.ui.expenses.GlobalExpensesViewModel";

      static String com_example_gestaobilhares_ui_settlement_SettlementViewModel = "com.example.gestaobilhares.ui.settlement.SettlementViewModel";

      static String com_example_gestaobilhares_ui_inventory_vehicles_VehiclesViewModel = "com.example.gestaobilhares.ui.inventory.vehicles.VehiclesViewModel";

      static String com_example_gestaobilhares_ui_mesas_MesasReformadasViewModel = "com.example.gestaobilhares.ui.mesas.MesasReformadasViewModel";

      static String com_example_gestaobilhares_ui_inventory_vehicles_VehicleDetailViewModel = "com.example.gestaobilhares.ui.inventory.vehicles.VehicleDetailViewModel";

      static String com_example_gestaobilhares_ui_cycles_CycleClientsViewModel = "com.example.gestaobilhares.ui.cycles.CycleClientsViewModel";

      static String com_example_gestaobilhares_ui_metas_MetasViewModel = "com.example.gestaobilhares.ui.metas.MetasViewModel";

      static String com_example_gestaobilhares_ui_cycles_CycleExpensesViewModel = "com.example.gestaobilhares.ui.cycles.CycleExpensesViewModel";

      static String com_example_gestaobilhares_ui_dashboard_DashboardViewModel = "com.example.gestaobilhares.ui.dashboard.DashboardViewModel";

      static String com_example_gestaobilhares_ui_colaboradores_ColaboradorManagementViewModel = "com.example.gestaobilhares.ui.colaboradores.ColaboradorManagementViewModel";

      static String com_example_gestaobilhares_ui_cycles_CycleManagementViewModel = "com.example.gestaobilhares.ui.cycles.CycleManagementViewModel";

      static String com_example_gestaobilhares_ui_routes_ClientSelectionViewModel = "com.example.gestaobilhares.ui.routes.ClientSelectionViewModel";

      static String com_example_gestaobilhares_ui_cycles_CycleReceiptsViewModel = "com.example.gestaobilhares.ui.cycles.CycleReceiptsViewModel";

      static String com_example_gestaobilhares_ui_mesas_EditMesaViewModel = "com.example.gestaobilhares.ui.mesas.EditMesaViewModel";

      static String com_example_gestaobilhares_ui_mesas_MesasDepositoViewModel = "com.example.gestaobilhares.ui.mesas.MesasDepositoViewModel";

      static String com_example_gestaobilhares_ui_mesas_RotaMesasViewModel = "com.example.gestaobilhares.ui.mesas.RotaMesasViewModel";

      static String com_example_gestaobilhares_ui_contracts_SignatureCaptureViewModel = "com.example.gestaobilhares.ui.contracts.SignatureCaptureViewModel";

      static String com_example_gestaobilhares_ui_reports_ClosureReportViewModel = "com.example.gestaobilhares.ui.reports.ClosureReportViewModel";

      static String com_example_gestaobilhares_ui_mesas_HistoricoManutencaoMesaViewModel = "com.example.gestaobilhares.ui.mesas.HistoricoManutencaoMesaViewModel";

      static String com_example_gestaobilhares_ui_metas_MetaCadastroViewModel = "com.example.gestaobilhares.ui.metas.MetaCadastroViewModel";

      static String com_example_gestaobilhares_ui_mesas_NovaReformaViewModel = "com.example.gestaobilhares.ui.mesas.NovaReformaViewModel";

      static String com_example_gestaobilhares_ui_clients_CycleHistoryViewModel = "com.example.gestaobilhares.ui.clients.CycleHistoryViewModel";

      static String com_example_gestaobilhares_ui_contracts_RepresentanteLegalSignatureViewModel = "com.example.gestaobilhares.ui.contracts.RepresentanteLegalSignatureViewModel";

      static String com_example_gestaobilhares_ui_contracts_AditivoSignatureViewModel = "com.example.gestaobilhares.ui.contracts.AditivoSignatureViewModel";

      static String com_example_gestaobilhares_ui_routes_RoutesViewModel = "com.example.gestaobilhares.ui.routes.RoutesViewModel";

      static String com_example_gestaobilhares_ui_mesas_HistoricoMesasVendidasViewModel = "com.example.gestaobilhares.ui.mesas.HistoricoMesasVendidasViewModel";

      static String com_example_gestaobilhares_ui_auth_AuthViewModel = "com.example.gestaobilhares.ui.auth.AuthViewModel";

      static String com_example_gestaobilhares_ui_expenses_ExpenseRegisterViewModel = "com.example.gestaobilhares.ui.expenses.ExpenseRegisterViewModel";

      static String com_example_gestaobilhares_ui_clients_ClientDetailViewModel = "com.example.gestaobilhares.ui.clients.ClientDetailViewModel";

      static String com_example_gestaobilhares_ui_inventory_stock_StockViewModel = "com.example.gestaobilhares.ui.inventory.stock.StockViewModel";

      @KeepFieldType
      RouteManagementViewModel com_example_gestaobilhares_ui_routes_management_RouteManagementViewModel2;

      @KeepFieldType
      TransferClientViewModel com_example_gestaobilhares_ui_routes_TransferClientViewModel2;

      @KeepFieldType
      ExpenseHistoryViewModel com_example_gestaobilhares_ui_expenses_ExpenseHistoryViewModel2;

      @KeepFieldType
      ClientListViewModel com_example_gestaobilhares_ui_clients_ClientListViewModel2;

      @KeepFieldType
      ContractManagementViewModel com_example_gestaobilhares_ui_contracts_ContractManagementViewModel2;

      @KeepFieldType
      ContractGenerationViewModel com_example_gestaobilhares_ui_contracts_ContractGenerationViewModel2;

      @KeepFieldType
      SettlementDetailViewModel com_example_gestaobilhares_ui_settlement_SettlementDetailViewModel2;

      @KeepFieldType
      GerenciarMesasViewModel com_example_gestaobilhares_ui_mesas_GerenciarMesasViewModel2;

      @KeepFieldType
      ClientRegisterViewModel com_example_gestaobilhares_ui_clients_ClientRegisterViewModel2;

      @KeepFieldType
      CadastroMesaViewModel com_example_gestaobilhares_ui_mesas_CadastroMesaViewModel2;

      @KeepFieldType
      EquipmentsViewModel com_example_gestaobilhares_ui_inventory_equipments_EquipmentsViewModel2;

      @KeepFieldType
      GlobalExpensesViewModel com_example_gestaobilhares_ui_expenses_GlobalExpensesViewModel2;

      @KeepFieldType
      SettlementViewModel com_example_gestaobilhares_ui_settlement_SettlementViewModel2;

      @KeepFieldType
      VehiclesViewModel com_example_gestaobilhares_ui_inventory_vehicles_VehiclesViewModel2;

      @KeepFieldType
      MesasReformadasViewModel com_example_gestaobilhares_ui_mesas_MesasReformadasViewModel2;

      @KeepFieldType
      VehicleDetailViewModel com_example_gestaobilhares_ui_inventory_vehicles_VehicleDetailViewModel2;

      @KeepFieldType
      CycleClientsViewModel com_example_gestaobilhares_ui_cycles_CycleClientsViewModel2;

      @KeepFieldType
      MetasViewModel com_example_gestaobilhares_ui_metas_MetasViewModel2;

      @KeepFieldType
      CycleExpensesViewModel com_example_gestaobilhares_ui_cycles_CycleExpensesViewModel2;

      @KeepFieldType
      DashboardViewModel com_example_gestaobilhares_ui_dashboard_DashboardViewModel2;

      @KeepFieldType
      ColaboradorManagementViewModel com_example_gestaobilhares_ui_colaboradores_ColaboradorManagementViewModel2;

      @KeepFieldType
      CycleManagementViewModel com_example_gestaobilhares_ui_cycles_CycleManagementViewModel2;

      @KeepFieldType
      ClientSelectionViewModel com_example_gestaobilhares_ui_routes_ClientSelectionViewModel2;

      @KeepFieldType
      CycleReceiptsViewModel com_example_gestaobilhares_ui_cycles_CycleReceiptsViewModel2;

      @KeepFieldType
      EditMesaViewModel com_example_gestaobilhares_ui_mesas_EditMesaViewModel2;

      @KeepFieldType
      MesasDepositoViewModel com_example_gestaobilhares_ui_mesas_MesasDepositoViewModel2;

      @KeepFieldType
      RotaMesasViewModel com_example_gestaobilhares_ui_mesas_RotaMesasViewModel2;

      @KeepFieldType
      SignatureCaptureViewModel com_example_gestaobilhares_ui_contracts_SignatureCaptureViewModel2;

      @KeepFieldType
      ClosureReportViewModel com_example_gestaobilhares_ui_reports_ClosureReportViewModel2;

      @KeepFieldType
      HistoricoManutencaoMesaViewModel com_example_gestaobilhares_ui_mesas_HistoricoManutencaoMesaViewModel2;

      @KeepFieldType
      MetaCadastroViewModel com_example_gestaobilhares_ui_metas_MetaCadastroViewModel2;

      @KeepFieldType
      NovaReformaViewModel com_example_gestaobilhares_ui_mesas_NovaReformaViewModel2;

      @KeepFieldType
      CycleHistoryViewModel com_example_gestaobilhares_ui_clients_CycleHistoryViewModel2;

      @KeepFieldType
      RepresentanteLegalSignatureViewModel com_example_gestaobilhares_ui_contracts_RepresentanteLegalSignatureViewModel2;

      @KeepFieldType
      AditivoSignatureViewModel com_example_gestaobilhares_ui_contracts_AditivoSignatureViewModel2;

      @KeepFieldType
      RoutesViewModel com_example_gestaobilhares_ui_routes_RoutesViewModel2;

      @KeepFieldType
      HistoricoMesasVendidasViewModel com_example_gestaobilhares_ui_mesas_HistoricoMesasVendidasViewModel2;

      @KeepFieldType
      AuthViewModel com_example_gestaobilhares_ui_auth_AuthViewModel2;

      @KeepFieldType
      ExpenseRegisterViewModel com_example_gestaobilhares_ui_expenses_ExpenseRegisterViewModel2;

      @KeepFieldType
      ClientDetailViewModel com_example_gestaobilhares_ui_clients_ClientDetailViewModel2;

      @KeepFieldType
      StockViewModel com_example_gestaobilhares_ui_inventory_stock_StockViewModel2;
    }
  }

  private static final class ViewModelCImpl extends GestaoBilharesApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AditivoSignatureViewModel> aditivoSignatureViewModelProvider;

    private Provider<AuthViewModel> authViewModelProvider;

    private Provider<CadastroMesaViewModel> cadastroMesaViewModelProvider;

    private Provider<ClientDetailViewModel> clientDetailViewModelProvider;

    private Provider<ClientListViewModel> clientListViewModelProvider;

    private Provider<ClientRegisterViewModel> clientRegisterViewModelProvider;

    private Provider<ClientSelectionViewModel> clientSelectionViewModelProvider;

    private Provider<ClosureReportViewModel> closureReportViewModelProvider;

    private Provider<ColaboradorManagementViewModel> colaboradorManagementViewModelProvider;

    private Provider<ContractGenerationViewModel> contractGenerationViewModelProvider;

    private Provider<ContractManagementViewModel> contractManagementViewModelProvider;

    private Provider<CycleClientsViewModel> cycleClientsViewModelProvider;

    private Provider<CycleExpensesViewModel> cycleExpensesViewModelProvider;

    private Provider<CycleHistoryViewModel> cycleHistoryViewModelProvider;

    private Provider<CycleManagementViewModel> cycleManagementViewModelProvider;

    private Provider<CycleReceiptsViewModel> cycleReceiptsViewModelProvider;

    private Provider<DashboardViewModel> dashboardViewModelProvider;

    private Provider<EditMesaViewModel> editMesaViewModelProvider;

    private Provider<EquipmentsViewModel> equipmentsViewModelProvider;

    private Provider<ExpenseHistoryViewModel> expenseHistoryViewModelProvider;

    private Provider<ExpenseRegisterViewModel> expenseRegisterViewModelProvider;

    private Provider<GerenciarMesasViewModel> gerenciarMesasViewModelProvider;

    private Provider<GlobalExpensesViewModel> globalExpensesViewModelProvider;

    private Provider<HistoricoManutencaoMesaViewModel> historicoManutencaoMesaViewModelProvider;

    private Provider<HistoricoMesasVendidasViewModel> historicoMesasVendidasViewModelProvider;

    private Provider<MesasDepositoViewModel> mesasDepositoViewModelProvider;

    private Provider<MesasReformadasViewModel> mesasReformadasViewModelProvider;

    private Provider<MetaCadastroViewModel> metaCadastroViewModelProvider;

    private Provider<MetasViewModel> metasViewModelProvider;

    private Provider<NovaReformaViewModel> novaReformaViewModelProvider;

    private Provider<RepresentanteLegalSignatureViewModel> representanteLegalSignatureViewModelProvider;

    private Provider<RotaMesasViewModel> rotaMesasViewModelProvider;

    private Provider<RouteManagementViewModel> routeManagementViewModelProvider;

    private Provider<RoutesViewModel> routesViewModelProvider;

    private Provider<SettlementDetailViewModel> settlementDetailViewModelProvider;

    private Provider<SettlementViewModel> settlementViewModelProvider;

    private Provider<SignatureCaptureViewModel> signatureCaptureViewModelProvider;

    private Provider<StockViewModel> stockViewModelProvider;

    private Provider<TransferClientViewModel> transferClientViewModelProvider;

    private Provider<VehicleDetailViewModel> vehicleDetailViewModelProvider;

    private Provider<VehiclesViewModel> vehiclesViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.aditivoSignatureViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.cadastroMesaViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.clientDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.clientListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.clientRegisterViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.clientSelectionViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.closureReportViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.colaboradorManagementViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.contractGenerationViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.contractManagementViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.cycleClientsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
      this.cycleExpensesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 12);
      this.cycleHistoryViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 13);
      this.cycleManagementViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 14);
      this.cycleReceiptsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 15);
      this.dashboardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 16);
      this.editMesaViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 17);
      this.equipmentsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 18);
      this.expenseHistoryViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 19);
      this.expenseRegisterViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 20);
      this.gerenciarMesasViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 21);
      this.globalExpensesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 22);
      this.historicoManutencaoMesaViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 23);
      this.historicoMesasVendidasViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 24);
      this.mesasDepositoViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 25);
      this.mesasReformadasViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 26);
      this.metaCadastroViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 27);
      this.metasViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 28);
      this.novaReformaViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 29);
      this.representanteLegalSignatureViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 30);
      this.rotaMesasViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 31);
      this.routeManagementViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 32);
      this.routesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 33);
      this.settlementDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 34);
      this.settlementViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 35);
      this.signatureCaptureViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 36);
      this.stockViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 37);
      this.transferClientViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 38);
      this.vehicleDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 39);
      this.vehiclesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 40);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(41).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_contracts_AditivoSignatureViewModel, ((Provider) aditivoSignatureViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_auth_AuthViewModel, ((Provider) authViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_CadastroMesaViewModel, ((Provider) cadastroMesaViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_clients_ClientDetailViewModel, ((Provider) clientDetailViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_clients_ClientListViewModel, ((Provider) clientListViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_clients_ClientRegisterViewModel, ((Provider) clientRegisterViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_routes_ClientSelectionViewModel, ((Provider) clientSelectionViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_reports_ClosureReportViewModel, ((Provider) closureReportViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_colaboradores_ColaboradorManagementViewModel, ((Provider) colaboradorManagementViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_contracts_ContractGenerationViewModel, ((Provider) contractGenerationViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_contracts_ContractManagementViewModel, ((Provider) contractManagementViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_cycles_CycleClientsViewModel, ((Provider) cycleClientsViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_cycles_CycleExpensesViewModel, ((Provider) cycleExpensesViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_clients_CycleHistoryViewModel, ((Provider) cycleHistoryViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_cycles_CycleManagementViewModel, ((Provider) cycleManagementViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_cycles_CycleReceiptsViewModel, ((Provider) cycleReceiptsViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_dashboard_DashboardViewModel, ((Provider) dashboardViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_EditMesaViewModel, ((Provider) editMesaViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_inventory_equipments_EquipmentsViewModel, ((Provider) equipmentsViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_expenses_ExpenseHistoryViewModel, ((Provider) expenseHistoryViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_expenses_ExpenseRegisterViewModel, ((Provider) expenseRegisterViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_GerenciarMesasViewModel, ((Provider) gerenciarMesasViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_expenses_GlobalExpensesViewModel, ((Provider) globalExpensesViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_HistoricoManutencaoMesaViewModel, ((Provider) historicoManutencaoMesaViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_HistoricoMesasVendidasViewModel, ((Provider) historicoMesasVendidasViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_MesasDepositoViewModel, ((Provider) mesasDepositoViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_MesasReformadasViewModel, ((Provider) mesasReformadasViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_metas_MetaCadastroViewModel, ((Provider) metaCadastroViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_metas_MetasViewModel, ((Provider) metasViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_NovaReformaViewModel, ((Provider) novaReformaViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_contracts_RepresentanteLegalSignatureViewModel, ((Provider) representanteLegalSignatureViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_mesas_RotaMesasViewModel, ((Provider) rotaMesasViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_routes_management_RouteManagementViewModel, ((Provider) routeManagementViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_routes_RoutesViewModel, ((Provider) routesViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_settlement_SettlementDetailViewModel, ((Provider) settlementDetailViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_settlement_SettlementViewModel, ((Provider) settlementViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_contracts_SignatureCaptureViewModel, ((Provider) signatureCaptureViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_inventory_stock_StockViewModel, ((Provider) stockViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_routes_TransferClientViewModel, ((Provider) transferClientViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_inventory_vehicles_VehicleDetailViewModel, ((Provider) vehicleDetailViewModelProvider)).put(LazyClassKeyProvider.com_example_gestaobilhares_ui_inventory_vehicles_VehiclesViewModel, ((Provider) vehiclesViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_example_gestaobilhares_ui_auth_AuthViewModel = "com.example.gestaobilhares.ui.auth.AuthViewModel";

      static String com_example_gestaobilhares_ui_dashboard_DashboardViewModel = "com.example.gestaobilhares.ui.dashboard.DashboardViewModel";

      static String com_example_gestaobilhares_ui_cycles_CycleClientsViewModel = "com.example.gestaobilhares.ui.cycles.CycleClientsViewModel";

      static String com_example_gestaobilhares_ui_clients_ClientListViewModel = "com.example.gestaobilhares.ui.clients.ClientListViewModel";

      static String com_example_gestaobilhares_ui_settlement_SettlementViewModel = "com.example.gestaobilhares.ui.settlement.SettlementViewModel";

      static String com_example_gestaobilhares_ui_clients_ClientRegisterViewModel = "com.example.gestaobilhares.ui.clients.ClientRegisterViewModel";

      static String com_example_gestaobilhares_ui_routes_ClientSelectionViewModel = "com.example.gestaobilhares.ui.routes.ClientSelectionViewModel";

      static String com_example_gestaobilhares_ui_routes_RoutesViewModel = "com.example.gestaobilhares.ui.routes.RoutesViewModel";

      static String com_example_gestaobilhares_ui_metas_MetasViewModel = "com.example.gestaobilhares.ui.metas.MetasViewModel";

      static String com_example_gestaobilhares_ui_mesas_EditMesaViewModel = "com.example.gestaobilhares.ui.mesas.EditMesaViewModel";

      static String com_example_gestaobilhares_ui_mesas_HistoricoMesasVendidasViewModel = "com.example.gestaobilhares.ui.mesas.HistoricoMesasVendidasViewModel";

      static String com_example_gestaobilhares_ui_contracts_ContractGenerationViewModel = "com.example.gestaobilhares.ui.contracts.ContractGenerationViewModel";

      static String com_example_gestaobilhares_ui_mesas_NovaReformaViewModel = "com.example.gestaobilhares.ui.mesas.NovaReformaViewModel";

      static String com_example_gestaobilhares_ui_expenses_GlobalExpensesViewModel = "com.example.gestaobilhares.ui.expenses.GlobalExpensesViewModel";

      static String com_example_gestaobilhares_ui_mesas_HistoricoManutencaoMesaViewModel = "com.example.gestaobilhares.ui.mesas.HistoricoManutencaoMesaViewModel";

      static String com_example_gestaobilhares_ui_contracts_SignatureCaptureViewModel = "com.example.gestaobilhares.ui.contracts.SignatureCaptureViewModel";

      static String com_example_gestaobilhares_ui_cycles_CycleReceiptsViewModel = "com.example.gestaobilhares.ui.cycles.CycleReceiptsViewModel";

      static String com_example_gestaobilhares_ui_mesas_CadastroMesaViewModel = "com.example.gestaobilhares.ui.mesas.CadastroMesaViewModel";

      static String com_example_gestaobilhares_ui_expenses_ExpenseHistoryViewModel = "com.example.gestaobilhares.ui.expenses.ExpenseHistoryViewModel";

      static String com_example_gestaobilhares_ui_inventory_vehicles_VehiclesViewModel = "com.example.gestaobilhares.ui.inventory.vehicles.VehiclesViewModel";

      static String com_example_gestaobilhares_ui_mesas_MesasDepositoViewModel = "com.example.gestaobilhares.ui.mesas.MesasDepositoViewModel";

      static String com_example_gestaobilhares_ui_cycles_CycleExpensesViewModel = "com.example.gestaobilhares.ui.cycles.CycleExpensesViewModel";

      static String com_example_gestaobilhares_ui_expenses_ExpenseRegisterViewModel = "com.example.gestaobilhares.ui.expenses.ExpenseRegisterViewModel";

      static String com_example_gestaobilhares_ui_contracts_RepresentanteLegalSignatureViewModel = "com.example.gestaobilhares.ui.contracts.RepresentanteLegalSignatureViewModel";

      static String com_example_gestaobilhares_ui_inventory_stock_StockViewModel = "com.example.gestaobilhares.ui.inventory.stock.StockViewModel";

      static String com_example_gestaobilhares_ui_routes_TransferClientViewModel = "com.example.gestaobilhares.ui.routes.TransferClientViewModel";

      static String com_example_gestaobilhares_ui_mesas_GerenciarMesasViewModel = "com.example.gestaobilhares.ui.mesas.GerenciarMesasViewModel";

      static String com_example_gestaobilhares_ui_contracts_ContractManagementViewModel = "com.example.gestaobilhares.ui.contracts.ContractManagementViewModel";

      static String com_example_gestaobilhares_ui_inventory_vehicles_VehicleDetailViewModel = "com.example.gestaobilhares.ui.inventory.vehicles.VehicleDetailViewModel";

      static String com_example_gestaobilhares_ui_metas_MetaCadastroViewModel = "com.example.gestaobilhares.ui.metas.MetaCadastroViewModel";

      static String com_example_gestaobilhares_ui_mesas_RotaMesasViewModel = "com.example.gestaobilhares.ui.mesas.RotaMesasViewModel";

      static String com_example_gestaobilhares_ui_cycles_CycleManagementViewModel = "com.example.gestaobilhares.ui.cycles.CycleManagementViewModel";

      static String com_example_gestaobilhares_ui_inventory_equipments_EquipmentsViewModel = "com.example.gestaobilhares.ui.inventory.equipments.EquipmentsViewModel";

      static String com_example_gestaobilhares_ui_mesas_MesasReformadasViewModel = "com.example.gestaobilhares.ui.mesas.MesasReformadasViewModel";

      static String com_example_gestaobilhares_ui_clients_CycleHistoryViewModel = "com.example.gestaobilhares.ui.clients.CycleHistoryViewModel";

      static String com_example_gestaobilhares_ui_settlement_SettlementDetailViewModel = "com.example.gestaobilhares.ui.settlement.SettlementDetailViewModel";

      static String com_example_gestaobilhares_ui_contracts_AditivoSignatureViewModel = "com.example.gestaobilhares.ui.contracts.AditivoSignatureViewModel";

      static String com_example_gestaobilhares_ui_reports_ClosureReportViewModel = "com.example.gestaobilhares.ui.reports.ClosureReportViewModel";

      static String com_example_gestaobilhares_ui_clients_ClientDetailViewModel = "com.example.gestaobilhares.ui.clients.ClientDetailViewModel";

      static String com_example_gestaobilhares_ui_routes_management_RouteManagementViewModel = "com.example.gestaobilhares.ui.routes.management.RouteManagementViewModel";

      static String com_example_gestaobilhares_ui_colaboradores_ColaboradorManagementViewModel = "com.example.gestaobilhares.ui.colaboradores.ColaboradorManagementViewModel";

      @KeepFieldType
      AuthViewModel com_example_gestaobilhares_ui_auth_AuthViewModel2;

      @KeepFieldType
      DashboardViewModel com_example_gestaobilhares_ui_dashboard_DashboardViewModel2;

      @KeepFieldType
      CycleClientsViewModel com_example_gestaobilhares_ui_cycles_CycleClientsViewModel2;

      @KeepFieldType
      ClientListViewModel com_example_gestaobilhares_ui_clients_ClientListViewModel2;

      @KeepFieldType
      SettlementViewModel com_example_gestaobilhares_ui_settlement_SettlementViewModel2;

      @KeepFieldType
      ClientRegisterViewModel com_example_gestaobilhares_ui_clients_ClientRegisterViewModel2;

      @KeepFieldType
      ClientSelectionViewModel com_example_gestaobilhares_ui_routes_ClientSelectionViewModel2;

      @KeepFieldType
      RoutesViewModel com_example_gestaobilhares_ui_routes_RoutesViewModel2;

      @KeepFieldType
      MetasViewModel com_example_gestaobilhares_ui_metas_MetasViewModel2;

      @KeepFieldType
      EditMesaViewModel com_example_gestaobilhares_ui_mesas_EditMesaViewModel2;

      @KeepFieldType
      HistoricoMesasVendidasViewModel com_example_gestaobilhares_ui_mesas_HistoricoMesasVendidasViewModel2;

      @KeepFieldType
      ContractGenerationViewModel com_example_gestaobilhares_ui_contracts_ContractGenerationViewModel2;

      @KeepFieldType
      NovaReformaViewModel com_example_gestaobilhares_ui_mesas_NovaReformaViewModel2;

      @KeepFieldType
      GlobalExpensesViewModel com_example_gestaobilhares_ui_expenses_GlobalExpensesViewModel2;

      @KeepFieldType
      HistoricoManutencaoMesaViewModel com_example_gestaobilhares_ui_mesas_HistoricoManutencaoMesaViewModel2;

      @KeepFieldType
      SignatureCaptureViewModel com_example_gestaobilhares_ui_contracts_SignatureCaptureViewModel2;

      @KeepFieldType
      CycleReceiptsViewModel com_example_gestaobilhares_ui_cycles_CycleReceiptsViewModel2;

      @KeepFieldType
      CadastroMesaViewModel com_example_gestaobilhares_ui_mesas_CadastroMesaViewModel2;

      @KeepFieldType
      ExpenseHistoryViewModel com_example_gestaobilhares_ui_expenses_ExpenseHistoryViewModel2;

      @KeepFieldType
      VehiclesViewModel com_example_gestaobilhares_ui_inventory_vehicles_VehiclesViewModel2;

      @KeepFieldType
      MesasDepositoViewModel com_example_gestaobilhares_ui_mesas_MesasDepositoViewModel2;

      @KeepFieldType
      CycleExpensesViewModel com_example_gestaobilhares_ui_cycles_CycleExpensesViewModel2;

      @KeepFieldType
      ExpenseRegisterViewModel com_example_gestaobilhares_ui_expenses_ExpenseRegisterViewModel2;

      @KeepFieldType
      RepresentanteLegalSignatureViewModel com_example_gestaobilhares_ui_contracts_RepresentanteLegalSignatureViewModel2;

      @KeepFieldType
      StockViewModel com_example_gestaobilhares_ui_inventory_stock_StockViewModel2;

      @KeepFieldType
      TransferClientViewModel com_example_gestaobilhares_ui_routes_TransferClientViewModel2;

      @KeepFieldType
      GerenciarMesasViewModel com_example_gestaobilhares_ui_mesas_GerenciarMesasViewModel2;

      @KeepFieldType
      ContractManagementViewModel com_example_gestaobilhares_ui_contracts_ContractManagementViewModel2;

      @KeepFieldType
      VehicleDetailViewModel com_example_gestaobilhares_ui_inventory_vehicles_VehicleDetailViewModel2;

      @KeepFieldType
      MetaCadastroViewModel com_example_gestaobilhares_ui_metas_MetaCadastroViewModel2;

      @KeepFieldType
      RotaMesasViewModel com_example_gestaobilhares_ui_mesas_RotaMesasViewModel2;

      @KeepFieldType
      CycleManagementViewModel com_example_gestaobilhares_ui_cycles_CycleManagementViewModel2;

      @KeepFieldType
      EquipmentsViewModel com_example_gestaobilhares_ui_inventory_equipments_EquipmentsViewModel2;

      @KeepFieldType
      MesasReformadasViewModel com_example_gestaobilhares_ui_mesas_MesasReformadasViewModel2;

      @KeepFieldType
      CycleHistoryViewModel com_example_gestaobilhares_ui_clients_CycleHistoryViewModel2;

      @KeepFieldType
      SettlementDetailViewModel com_example_gestaobilhares_ui_settlement_SettlementDetailViewModel2;

      @KeepFieldType
      AditivoSignatureViewModel com_example_gestaobilhares_ui_contracts_AditivoSignatureViewModel2;

      @KeepFieldType
      ClosureReportViewModel com_example_gestaobilhares_ui_reports_ClosureReportViewModel2;

      @KeepFieldType
      ClientDetailViewModel com_example_gestaobilhares_ui_clients_ClientDetailViewModel2;

      @KeepFieldType
      RouteManagementViewModel com_example_gestaobilhares_ui_routes_management_RouteManagementViewModel2;

      @KeepFieldType
      ColaboradorManagementViewModel com_example_gestaobilhares_ui_colaboradores_ColaboradorManagementViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.example.gestaobilhares.ui.contracts.AditivoSignatureViewModel 
          return (T) new AditivoSignatureViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 1: // com.example.gestaobilhares.ui.auth.AuthViewModel 
          return (T) new AuthViewModel(singletonCImpl.provideAppRepositoryProvider.get(), singletonCImpl.provideNetworkUtilsProvider.get(), singletonCImpl.provideUserSessionManagerProvider.get());

          case 2: // com.example.gestaobilhares.ui.mesas.CadastroMesaViewModel 
          return (T) new CadastroMesaViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 3: // com.example.gestaobilhares.ui.clients.ClientDetailViewModel 
          return (T) new ClientDetailViewModel(singletonCImpl.provideAppRepositoryProvider.get(), singletonCImpl.provideUserSessionManagerProvider.get(), singletonCImpl.provideSyncRepositoryProvider.get());

          case 4: // com.example.gestaobilhares.ui.clients.ClientListViewModel 
          return (T) new ClientListViewModel(singletonCImpl.provideAppRepositoryProvider.get(), singletonCImpl.provideUserSessionManagerProvider.get());

          case 5: // com.example.gestaobilhares.ui.clients.ClientRegisterViewModel 
          return (T) new ClientRegisterViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 6: // com.example.gestaobilhares.ui.routes.ClientSelectionViewModel 
          return (T) new ClientSelectionViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 7: // com.example.gestaobilhares.ui.reports.ClosureReportViewModel 
          return (T) new ClosureReportViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 8: // com.example.gestaobilhares.ui.colaboradores.ColaboradorManagementViewModel 
          return (T) new ColaboradorManagementViewModel(singletonCImpl.provideAppRepositoryProvider.get(), singletonCImpl.provideUserSessionManagerProvider.get());

          case 9: // com.example.gestaobilhares.ui.contracts.ContractGenerationViewModel 
          return (T) new ContractGenerationViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 10: // com.example.gestaobilhares.ui.contracts.ContractManagementViewModel 
          return (T) new ContractManagementViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 11: // com.example.gestaobilhares.ui.cycles.CycleClientsViewModel 
          return (T) new CycleClientsViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 12: // com.example.gestaobilhares.ui.cycles.CycleExpensesViewModel 
          return (T) new CycleExpensesViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 13: // com.example.gestaobilhares.ui.clients.CycleHistoryViewModel 
          return (T) new CycleHistoryViewModel(singletonCImpl.provideCicloAcertoRepositoryProvider.get(), singletonCImpl.provideAppRepositoryProvider.get());

          case 14: // com.example.gestaobilhares.ui.cycles.CycleManagementViewModel 
          return (T) new CycleManagementViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 15: // com.example.gestaobilhares.ui.cycles.CycleReceiptsViewModel 
          return (T) new CycleReceiptsViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 16: // com.example.gestaobilhares.ui.dashboard.DashboardViewModel 
          return (T) new DashboardViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 17: // com.example.gestaobilhares.ui.mesas.EditMesaViewModel 
          return (T) new EditMesaViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 18: // com.example.gestaobilhares.ui.inventory.equipments.EquipmentsViewModel 
          return (T) new EquipmentsViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 19: // com.example.gestaobilhares.ui.expenses.ExpenseHistoryViewModel 
          return (T) new ExpenseHistoryViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 20: // com.example.gestaobilhares.ui.expenses.ExpenseRegisterViewModel 
          return (T) new ExpenseRegisterViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 21: // com.example.gestaobilhares.ui.mesas.GerenciarMesasViewModel 
          return (T) new GerenciarMesasViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 22: // com.example.gestaobilhares.ui.expenses.GlobalExpensesViewModel 
          return (T) new GlobalExpensesViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 23: // com.example.gestaobilhares.ui.mesas.HistoricoManutencaoMesaViewModel 
          return (T) new HistoricoManutencaoMesaViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 24: // com.example.gestaobilhares.ui.mesas.HistoricoMesasVendidasViewModel 
          return (T) new HistoricoMesasVendidasViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 25: // com.example.gestaobilhares.ui.mesas.MesasDepositoViewModel 
          return (T) new MesasDepositoViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 26: // com.example.gestaobilhares.ui.mesas.MesasReformadasViewModel 
          return (T) new MesasReformadasViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 27: // com.example.gestaobilhares.ui.metas.MetaCadastroViewModel 
          return (T) new MetaCadastroViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 28: // com.example.gestaobilhares.ui.metas.MetasViewModel 
          return (T) new MetasViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 29: // com.example.gestaobilhares.ui.mesas.NovaReformaViewModel 
          return (T) new NovaReformaViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 30: // com.example.gestaobilhares.ui.contracts.RepresentanteLegalSignatureViewModel 
          return (T) new RepresentanteLegalSignatureViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 31: // com.example.gestaobilhares.ui.mesas.RotaMesasViewModel 
          return (T) new RotaMesasViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 32: // com.example.gestaobilhares.ui.routes.management.RouteManagementViewModel 
          return (T) new RouteManagementViewModel(singletonCImpl.provideAppRepositoryProvider.get(), singletonCImpl.provideUserSessionManagerProvider.get());

          case 33: // com.example.gestaobilhares.ui.routes.RoutesViewModel 
          return (T) new RoutesViewModel(singletonCImpl.provideAppRepositoryProvider.get(), singletonCImpl.provideUserSessionManagerProvider.get());

          case 34: // com.example.gestaobilhares.ui.settlement.SettlementDetailViewModel 
          return (T) new SettlementDetailViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 35: // com.example.gestaobilhares.ui.settlement.SettlementViewModel 
          return (T) new SettlementViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 36: // com.example.gestaobilhares.ui.contracts.SignatureCaptureViewModel 
          return (T) new SignatureCaptureViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 37: // com.example.gestaobilhares.ui.inventory.stock.StockViewModel 
          return (T) new StockViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 38: // com.example.gestaobilhares.ui.routes.TransferClientViewModel 
          return (T) new TransferClientViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 39: // com.example.gestaobilhares.ui.inventory.vehicles.VehicleDetailViewModel 
          return (T) new VehicleDetailViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          case 40: // com.example.gestaobilhares.ui.inventory.vehicles.VehiclesViewModel 
          return (T) new VehiclesViewModel(singletonCImpl.provideAppRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends GestaoBilharesApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends GestaoBilharesApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends GestaoBilharesApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<AppDatabase> provideAppDatabaseProvider;

    private Provider<AppRepository> provideAppRepositoryProvider;

    private Provider<NetworkUtils> provideNetworkUtilsProvider;

    private Provider<FirebaseFirestore> provideFirebaseFirestoreProvider;

    private Provider<com.example.gestaobilhares.sync.utils.NetworkUtils> provideNetworkUtilsProvider2;

    private Provider<SyncRepository> provideSyncRepositoryProvider;

    private Provider<UserSessionManager> provideUserSessionManagerProvider;

    private Provider<AcertoRepository> provideAcertoRepositoryProvider;

    private Provider<ClienteRepository> provideClienteRepositoryProvider;

    private Provider<CicloAcertoRepository> provideCicloAcertoRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private CicloAcertoDao cicloAcertoDao() {
      return DatabaseModule_ProvideCicloDaoFactory.provideCicloDao(provideAppDatabaseProvider.get());
    }

    private DespesaDao despesaDao() {
      return DatabaseModule_ProvideDespesaDaoFactory.provideDespesaDao(provideAppDatabaseProvider.get());
    }

    private AcertoDao acertoDao() {
      return DatabaseModule_ProvideAcertoDaoFactory.provideAcertoDao(provideAppDatabaseProvider.get());
    }

    private ClienteDao clienteDao() {
      return DatabaseModule_ProvideClienteDaoFactory.provideClienteDao(provideAppDatabaseProvider.get());
    }

    private RotaDao rotaDao() {
      return DatabaseModule_ProvideRotaDaoFactory.provideRotaDao(provideAppDatabaseProvider.get());
    }

    private ColaboradorDao colaboradorDao() {
      return DatabaseModule_ProvideColaboradorDaoFactory.provideColaboradorDao(provideAppDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideAppDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 1));
      this.provideAppRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AppRepository>(singletonCImpl, 0));
      this.provideNetworkUtilsProvider = DoubleCheck.provider(new SwitchingProvider<NetworkUtils>(singletonCImpl, 2));
      this.provideFirebaseFirestoreProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseFirestore>(singletonCImpl, 4));
      this.provideNetworkUtilsProvider2 = DoubleCheck.provider(new SwitchingProvider<com.example.gestaobilhares.sync.utils.NetworkUtils>(singletonCImpl, 5));
      this.provideSyncRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SyncRepository>(singletonCImpl, 3));
      this.provideUserSessionManagerProvider = DoubleCheck.provider(new SwitchingProvider<UserSessionManager>(singletonCImpl, 6));
      this.provideAcertoRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AcertoRepository>(singletonCImpl, 8));
      this.provideClienteRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ClienteRepository>(singletonCImpl, 9));
      this.provideCicloAcertoRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<CicloAcertoRepository>(singletonCImpl, 7));
    }

    @Override
    public void injectGestaoBilharesApplication(
        GestaoBilharesApplication gestaoBilharesApplication) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.example.gestaobilhares.data.repository.AppRepository 
          return (T) RepositoryModule_ProvideAppRepositoryFactory.provideAppRepository(singletonCImpl.provideAppDatabaseProvider.get());

          case 1: // com.example.gestaobilhares.data.database.AppDatabase 
          return (T) DatabaseModule_ProvideAppDatabaseFactory.provideAppDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.example.gestaobilhares.core.utils.NetworkUtils 
          return (T) CoreModule_ProvideNetworkUtilsFactory.provideNetworkUtils(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.example.gestaobilhares.sync.SyncRepository 
          return (T) SyncModule_ProvideSyncRepositoryFactory.provideSyncRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideAppRepositoryProvider.get(), singletonCImpl.provideFirebaseFirestoreProvider.get(), singletonCImpl.provideNetworkUtilsProvider2.get());

          case 4: // com.google.firebase.firestore.FirebaseFirestore 
          return (T) RepositoryModule_ProvideFirebaseFirestoreFactory.provideFirebaseFirestore();

          case 5: // com.example.gestaobilhares.sync.utils.NetworkUtils 
          return (T) SyncModule_ProvideNetworkUtilsFactory.provideNetworkUtils(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.example.gestaobilhares.core.utils.UserSessionManager 
          return (T) CoreModule_ProvideUserSessionManagerFactory.provideUserSessionManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // com.example.gestaobilhares.data.repository.CicloAcertoRepository 
          return (T) RepositoryModule_ProvideCicloAcertoRepositoryFactory.provideCicloAcertoRepository(singletonCImpl.cicloAcertoDao(), singletonCImpl.despesaDao(), singletonCImpl.provideAcertoRepositoryProvider.get(), singletonCImpl.provideClienteRepositoryProvider.get(), singletonCImpl.rotaDao(), singletonCImpl.colaboradorDao());

          case 8: // com.example.gestaobilhares.data.repository.AcertoRepository 
          return (T) RepositoryModule_ProvideAcertoRepositoryFactory.provideAcertoRepository(singletonCImpl.acertoDao(), singletonCImpl.clienteDao());

          case 9: // com.example.gestaobilhares.data.repository.ClienteRepository 
          return (T) RepositoryModule_ProvideClienteRepositoryFactory.provideClienteRepository(singletonCImpl.clienteDao(), singletonCImpl.provideAppRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
