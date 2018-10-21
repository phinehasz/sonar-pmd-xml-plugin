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

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import org.sonar.api.rules.RulePriority;

import static com.google.common.collect.ImmutableMap.of;

public final class PmdLevelUtils {
  private static final BiMap<RulePriority, String> LEVELS_PER_PRIORITY = EnumHashBiMap.create(of(
      RulePriority.BLOCKER, "1",
      RulePriority.CRITICAL, "2",
      RulePriority.MAJOR, "3",
      RulePriority.MINOR, "4",
      RulePriority.INFO, "5"));

  private PmdLevelUtils() {
    // only static methods
  }

  public static RulePriority fromLevel(String level) {
    return LEVELS_PER_PRIORITY.inverse().get(level);
  }

  public static String toLevel(RulePriority priority) {
    return LEVELS_PER_PRIORITY.get(priority);
  }
}
