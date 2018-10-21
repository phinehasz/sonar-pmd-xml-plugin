/*
 * SonarQube PMD Plugin
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.pmd;

import com.google.common.collect.ImmutableList;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;

import java.util.List;

public class PmdPlugin extends SonarPlugin {

  @Override
  public List getExtensions() {
    return ImmutableList.of(
      PropertyDefinition.builder(PmdConfiguration.PROPERTY_GENERATE_XML)
        .defaultValue("false")
        .name("Generate XML Report")
        .hidden()
        .build(),

      PmdSensor.class,
      PmdConfiguration.class,
      PmdExecutor.class,
      PmdRulesDefinition.class,
      PmdUnitTestsRulesDefinition.class,
      PmdProfileExporter.class,
      PmdProfileImporter.class,
      PmdViolationRecorder.class);
  }

}
