package com.example.gestaobilhares;

import com.example.gestaobilhares.core.utils.NetworkUtils;
import com.example.gestaobilhares.data.repository.AppRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<AppRepository> appRepositoryProvider;

  private final Provider<NetworkUtils> networkUtilsProvider;

  public MainActivity_MembersInjector(Provider<AppRepository> appRepositoryProvider,
      Provider<NetworkUtils> networkUtilsProvider) {
    this.appRepositoryProvider = appRepositoryProvider;
    this.networkUtilsProvider = networkUtilsProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<AppRepository> appRepositoryProvider,
      Provider<NetworkUtils> networkUtilsProvider) {
    return new MainActivity_MembersInjector(appRepositoryProvider, networkUtilsProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectAppRepository(instance, appRepositoryProvider.get());
    injectNetworkUtils(instance, networkUtilsProvider.get());
  }

  @InjectedFieldSignature("com.example.gestaobilhares.MainActivity.appRepository")
  public static void injectAppRepository(MainActivity instance, AppRepository appRepository) {
    instance.appRepository = appRepository;
  }

  @InjectedFieldSignature("com.example.gestaobilhares.MainActivity.networkUtils")
  public static void injectNetworkUtils(MainActivity instance, NetworkUtils networkUtils) {
    instance.networkUtils = networkUtils;
  }
}
