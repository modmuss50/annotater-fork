package net.fabricmc.filament;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
<<<<<<< Updated upstream
=======
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Sync;
>>>>>>> Stashed changes
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.JavadocMemberLevel;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
<<<<<<< Updated upstream
=======
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JvmVendorSpec;
>>>>>>> Stashed changes

import net.fabricmc.filament.task.CheckMappingsTask;
import net.fabricmc.filament.task.ConvertMappingsTask;
import net.fabricmc.filament.task.DecompileTask;
<<<<<<< Updated upstream
=======
import net.fabricmc.filament.task.DropCheckMappingsErrorsTask;
import net.fabricmc.filament.task.DropDecompileErrorsTask;
import net.fabricmc.filament.task.DropJavadocErrorsTask;
import net.fabricmc.filament.task.FixMappingsTask;
import net.fabricmc.filament.task.FormatMappingsTask;
import net.fabricmc.filament.task.JavaRuntimeJarTask;
import net.fabricmc.filament.task.unpick.CheckUnpickDefinitionsTask;
import net.fabricmc.filament.task.unpick.CombineUnpickDefinitionsTask;
import net.fabricmc.filament.task.unpick.UnpickJarTask;
import net.fabricmc.filament.util.FilamentConstants;
import net.fabricmc.loom.LoomGradleExtension;
>>>>>>> Stashed changes
import net.fabricmc.loom.api.LoomGradleExtensionAPI;

public final class FilamentGradlePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPluginManager().apply("net.fabricmc.fabric-loom");

		final TaskContainer tasks = project.getTasks();
		final Directory mappings = project.getLayout().getProjectDirectory().dir("mappings");
		// Minecraft is not obfuscated, so the named jars are the same as the official jars
		final FileCollection minecraftJars = project.getExtensions().getByType(LoomGradleExtensionAPI.class).getNamedMinecraftJars();
		final Configuration minecraftLibraries = project.getConfigurations().getByName("minecraftLibraries");

		final Configuration decompileClasspath = project.getConfigurations().create("decompileClasspath", configuration -> {
			configuration.setCanBeConsumed(false);
			configuration.extendsFrom(minecraftLibraries);
		});

<<<<<<< Updated upstream
=======
		var minecraftJavaVersion = project.getProviders().provider(() -> {
			var versionInfo = LoomGradleExtension.get(project).getMinecraftProvider().getVersionInfo();
			return JavaLanguageVersion.of(versionInfo.javaVersion().majorVersion());
		});
		var javaToolchains = project.getExtensions().getByType(JavaToolchainService.class);
		var javaLauncher = javaToolchains.launcherFor(spec -> {
			spec.getLanguageVersion().set(minecraftJavaVersion);
			spec.getVendor().set(JvmVendorSpec.MICROSOFT);
		});
		var javaRuntimeJar = tasks.register("javaRuntimeJar", JavaRuntimeJarTask.class, task -> {
			task.setGroup(ANNOTATER_TASK_GROUP);
			task.getJavaHome().set(javaLauncher.map(launcher -> launcher.getMetadata().getInstallationPath()));
			task.getOutput().set(buildDir.file("javaRuntime/java-runtime.jar"));
		});
		var syncRuntimeLibs = tasks.register("syncRuntimeLibs", Sync.class, task -> {
			task.setGroup(ANNOTATER_TASK_GROUP);
			task.setDescription("Syncs Minecraft dependencies and Java runtime classes, excluding the Minecraft jars.");
			task.from(decompileClasspath);
			task.from(javaRuntimeJar.flatMap(JavaRuntimeJarTask::getOutput));
			task.include("*.jar");
			task.into(buildDir.dir("runtime_libs"));
		});
		tasks.register("prepareMcSrc", task -> {
			task.setGroup(ANNOTATER_TASK_GROUP);
			task.setDescription("Prepares the project for mcsrc.");
			task.dependsOn(syncRuntimeLibs);
		});

		final Directory unpickDefinitions = project.getLayout().getProjectDirectory().dir("unpick-definitions");
		var checkUnpickDefinitions = tasks.register("checkUnpickDefinitions", CheckUnpickDefinitionsTask.class, task -> {
			task.setGroup(ANNOTATER_TASK_GROUP);
			task.getInput().set(unpickDefinitions);
			task.getClasspath().from(minecraftJars, decompileClasspath, javaRuntimeJar.flatMap(JavaRuntimeJarTask::getOutput));
		});
		var combineUnpickDefinitions = tasks.register("combineUnpickDefinitions", CombineUnpickDefinitionsTask.class, task -> {
			task.setGroup(ANNOTATER_TASK_GROUP);
			task.dependsOn(checkUnpickDefinitions);
			task.getInput().set(unpickDefinitions);
			task.getOutput().set(buildDir.file("unpick/definitions.unpick"));
		});
		var unpickJar = tasks.register("unpickJar", UnpickJarTask.class, task -> {
			task.setGroup(ANNOTATER_TASK_GROUP);
			task.setDescription("Applies unpick definitions to the Minecraft jar.");
			task.getInput().set(project.getLayout().file(minecraftJars.getElements().map(files -> {
				if (files.size() != 1) {
					throw new IllegalStateException("Unpick requires a single merged Minecraft jar");
				}

				return files.iterator().next().getAsFile();
			})));
			task.getUnpickDefinitions().set(combineUnpickDefinitions.flatMap(CombineUnpickDefinitionsTask::getOutput));
			task.getClasspath().from(decompileClasspath, javaRuntimeJar.flatMap(JavaRuntimeJarTask::getOutput));
			task.getOutput().set(buildDir.file("unpick/minecraft-unpicked.jar"));
		});
		tasks.register("decompileVineflower", DecompileTask.class, task -> {
			task.setGroup(ANNOTATER_TASK_GROUP);
			task.setDescription("Decompiles the whole game with unpick and javadocs applied.");
			task.getDecompileAll().set(true);
			task.getMappings().set(mappings);
			task.getMinecraftJars().from(unpickJar.flatMap(UnpickJarTask::getOutput));
			task.getLibraries().from(decompileClasspath);
			task.getOutput().set(buildDir.dir("vineflower"));
			task.getReport().set(buildDir.file("decompileVineflower/report.tsv"));
			task.getJavadocLines().set(buildDir.file("decompileVineflower/javadoc-lines.tsv"));
		});
		// Loom registers genSources after evaluation as a lifecycle task for its default decompiler.
		project.afterEvaluate(ignored -> tasks.named("genSources", task -> {
			task.setEnabled(false);
			// Gradle still runs the dependencies of a disabled task.
			task.setDependsOn(List.of());
		}));

>>>>>>> Stashed changes
		var checkMappings = tasks.register("checkMappings", CheckMappingsTask.class, task -> {
			task.getMappings().set(mappings);
			task.getMinecraftJars().from(minecraftJars);
		});
		tasks.register("convertMappings", ConvertMappingsTask.class, task -> {
			task.getMappings().set(mappings);
			task.getOutput().set(project.getLayout().getBuildDirectory().file("mappings.tiny"));
		});
		var decompile = tasks.register("decompile", DecompileTask.class, task -> {
			task.getMappings().set(mappings);
			task.getMinecraftJars().from(minecraftJars);
			task.getLibraries().from(decompileClasspath);
			task.getOutput().set(project.getLayout().getBuildDirectory().dir("decompiled"));
		});

		// Checks the javadocs in the decompiled sources of the documented classes
		tasks.named("javadoc", Javadoc.class, task -> {
			task.setSource(decompile);
			task.setClasspath(project.files(minecraftJars, decompileClasspath));

			// downcast, see https://github.com/gradle/gradle/issues/7038
			StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) task.getOptions();
			options.quiet();
			options.setMemberLevel(JavadocMemberLevel.PRIVATE);
			// Missing javadocs are allowed, as only some members are documented
			options.addBooleanOption("Xdoclint:all,-missing", true);
			// Decompiler errors in declarations would otherwise stop the javadocs from being checked
			options.addBooleanOption("-ignore-source-errors", true);
		});

		tasks.named("check", task -> task.dependsOn(checkMappings, "javadoc"));

		// checkMappings has no outputs, so it is not automatically ordered after clean
		checkMappings.configure(task -> task.mustRunAfter("clean"));
	}
}
