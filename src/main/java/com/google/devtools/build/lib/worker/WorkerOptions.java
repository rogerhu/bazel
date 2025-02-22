// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.worker;

import com.google.common.collect.Maps;
import com.google.devtools.build.lib.util.RamResourceConverter;
import com.google.devtools.build.lib.util.ResourceConverter;
import com.google.devtools.common.options.Converter;
import com.google.devtools.common.options.Converters;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionDocumentationCategory;
import com.google.devtools.common.options.OptionEffectTag;
import com.google.devtools.common.options.Options;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParsingException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Options related to worker processes. */
public class WorkerOptions extends OptionsBase {
  public static final WorkerOptions DEFAULTS = Options.getDefaults(WorkerOptions.class);

  @Option(
      name = "experimental_persistent_javac",
      defaultValue = "null",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
      help = "Enable the experimental persistent Java compiler.",
      expansion = {
        "--strategy=Javac=worker",
        "--strategy=JavaIjar=local",
        "--strategy=JavaDeployJar=local",
        "--strategy=JavaSourceJar=local",
        "--strategy=Turbine=local"
      })
  public Void experimentalPersistentJavac;

  /**
   * Defines a resource converter for named values in the form [name=]value, where the value is
   * {@link ResourceConverter.FLAG_SYNTAX}. If no name is provided (used when setting a default),
   * the empty string is used as the key. The default value for unspecified mnemonics is defined in
   * {@link WorkerPool.createWorkerPools}. "auto" currently returns the default.
   */
  public static class MultiResourceConverter extends Converter.Contextless<Entry<String, Integer>> {

    static final ResourceConverter valueConverter =
        new ResourceConverter(() -> 0, 0, Integer.MAX_VALUE);

    @Override
    public Map.Entry<String, Integer> convert(String input) throws OptionsParsingException {
      // TODO(steinman): Make auto value return a reasonable multiplier of host capacity.
      if (input == null || "null".equals(input) || "auto".equals(input)) {
        return Maps.immutableEntry(null, null);
      }
      int pos = input.indexOf('=');
      if (pos < 0) {
        return Maps.immutableEntry("", valueConverter.convert(input, /*conversionContext=*/ null));
      }
      String name = input.substring(0, pos);
      String value = input.substring(pos + 1);
      if ("auto".equals(value)) {
        return Maps.immutableEntry(name, null);
      }

      return Maps.immutableEntry(name, valueConverter.convert(value, /*conversionContext=*/ null));
    }

    @Override
    public String getTypeDescription() {
      return "[name=]value, where value is " + ResourceConverter.FLAG_SYNTAX;
    }
  }

  @Option(
      name = "worker_max_instances",
      converter = MultiResourceConverter.class,
      defaultValue = "auto",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
      help =
          "How many instances of a worker process (like the persistent Java compiler) may be "
              + "launched if you use the 'worker' strategy. May be specified as [name=value] to "
              + "give a different value per worker mnemonic. Takes "
              + ResourceConverter.FLAG_SYNTAX
              + ". 'auto' calculates a reasonable default based on machine capacity. "
              + "\"=value\" sets a default for unspecified mnemonics.",
      allowMultiple = true)
  public List<Map.Entry<String, Integer>> workerMaxInstances;

  @Option(
      name = "worker_max_multiplex_instances",
      oldName = "experimental_worker_max_multiplex_instances",
      oldNameWarning = false,
      converter = MultiResourceConverter.class,
      defaultValue = "null",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
      help =
          "How many WorkRequests a multiplex worker process may receive in parallel if you use the"
              + " 'worker' strategy with --experimental_worker_multiplex. May be specified as"
              + " [name=value] to give a different value per worker mnemonic. Takes "
              + ResourceConverter.FLAG_SYNTAX
              + ". 'auto' calculates a reasonable default based on machine capacity. "
              + "\"=value\" sets a default for unspecified mnemonics.",
      allowMultiple = true)
  public List<Map.Entry<String, Integer>> workerMaxMultiplexInstances;

  @Option(
      name = "high_priority_workers",
      defaultValue = "null",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "Mnemonics of workers to run with high priority. When high priority workers are running "
              + "all other workers are throttled.",
      allowMultiple = true)
  public List<String> highPriorityWorkers;

  @Option(
      name = "worker_quit_after_build",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
      help = "If enabled, all workers quit after a build is done.")
  public boolean workerQuitAfterBuild;

  @Option(
      name = "worker_verbose",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.UNKNOWN},
      help = "If enabled, prints verbose messages when workers are started, shutdown, ...")
  public boolean workerVerbose;

  @Option(
      name = "worker_extra_flag",
      converter = Converters.AssignmentConverter.class,
      defaultValue = "null",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
      help =
          "Extra command-flags that will be passed to worker processes in addition to "
              + "--persistent_worker, keyed by mnemonic (e.g. --worker_extra_flag=Javac=--debug.",
      allowMultiple = true)
  public List<Map.Entry<String, String>> workerExtraFlags;

  @Option(
      name = "worker_sandboxing",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help = "If enabled, workers will be executed in a sandboxed environment.")
  public boolean workerSandboxing;

  @Option(
      name = "experimental_worker_multiplex",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
      help =
          "If enabled, workers that support the experimental multiplexing feature will use that"
              + " feature.")
  public boolean workerMultiplex;

  @Option(
      name = "experimental_worker_cancellation",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help = "If enabled, Bazel may send cancellation requests to workers that support them.")
  public boolean workerCancellation;

  @Option(
      name = "experimental_worker_as_resource",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
      help = "If enabled, workers are acquired as resources from ResourceManager.")
  public boolean workerAsResource;

  @Option(
      name = "experimental_worker_multiplex_sandboxing",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "If enabled, multiplex workers will be sandboxed, using a separate sandbox directory"
              + " per work request. Only workers that have the 'supports-multiplex-sandboxing' "
              + "execution requirement will be sandboxed.")
  public boolean multiplexSandboxing;

  @Option(
      name = "experimental_worker_strict_flagfiles",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "If enabled, actions arguments for workers that do not follow the worker specification"
              + " will cause an error. Worker arguments must have exactly one @flagfile argument"
              + " as the last of its list of arguments.")
  public boolean strictFlagfiles;

  @Option(
      name = "experimental_total_worker_memory_limit_mb",
      converter = RamResourceConverter.class,
      defaultValue = "0",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION, OptionEffectTag.HOST_MACHINE_RESOURCE_OPTIMIZATIONS},
      help =
          "If this limit is greater than zero idle workers might be killed if the total memory"
              + " usage of all  workers exceed the limit.")
  public int totalWorkerMemoryLimitMb;

  @Option(
      name = "experimental_worker_sandbox_hardening",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.EXECUTION},
      help = "If enabled, workers are run in a hardened sandbox, if the implementation allows it.")
  public boolean sandboxHardening;
}
