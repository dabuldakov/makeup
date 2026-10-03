package com.example.makeup.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Фиксирует границы модулей makeup в коде. `auth` — базовый модуль аккаунтов,
 * `security` — только адаптер безопасности над `auth`, `account` — листовой
 * оркестратор удаления, от которого никто не зависит.
 */
@AnalyzeClasses(packages = "com.example.makeup", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundariesTest {

    private static final String AUTH = "com.example.makeup.auth..";
    private static final String ACCOUNT = "com.example.makeup.account..";
    private static final String NEWS = "com.example.makeup.news..";
    private static final String VIDEO = "com.example.makeup.video..";
    private static final String MEDIA = "com.example.makeup.media..";
    private static final String SECURITY = "com.example.makeup.security..";
    private static final String CONFIG = "com.example.makeup.config..";

    @ArchTest
    static final ArchRule auth_is_a_base_module = noClasses()
            .that().resideInAPackage(AUTH)
            .should().dependOnClassesThat().resideInAnyPackage(ACCOUNT, NEWS, VIDEO, MEDIA, SECURITY);

    @ArchTest
    static final ArchRule security_does_not_depend_on_domain_features = noClasses()
            .that().resideInAPackage(SECURITY)
            .should().dependOnClassesThat().resideInAnyPackage(ACCOUNT, NEWS, VIDEO, MEDIA);

    @ArchTest
    static final ArchRule account_is_a_leaf_orchestrator = noClasses()
            .that().resideOutsideOfPackage(ACCOUNT)
            .should().dependOnClassesThat().resideInAPackage(ACCOUNT);

    @ArchTest
    static final ArchRule config_is_independent_of_domain = noClasses()
            .that().resideInAPackage(CONFIG)
            .should().dependOnClassesThat().resideInAnyPackage(AUTH, ACCOUNT, NEWS, VIDEO, MEDIA, SECURITY);

    /**
     * Обработка превью идёт через порт {@code MediaJobHandler}: очередь медиа
     * ничего не знает о Video.
     */
    @ArchTest
    static final ArchRule media_does_not_depend_on_video = noClasses()
            .that().resideInAPackage(MEDIA)
            .should().dependOnClassesThat().resideInAPackage(VIDEO);

    /**
     * Ссылки на удаляемое видео снимаются событием {@code VideoDeletedEvent}:
     * video-модуль ничего не знает о news.
     */
    @ArchTest
    static final ArchRule video_does_not_depend_on_news = noClasses()
            .that().resideInAPackage(VIDEO)
            .should().dependOnClassesThat().resideInAPackage(NEWS);

    /** Граф модулей полностью ацикличен. */
    @ArchTest
    static final ArchRule module_slices_are_free_of_cycles = SlicesRuleDefinition.slices()
            .matching("com.example.makeup.(*)..")
            .should()
            .beFreeOfCycles();
}
