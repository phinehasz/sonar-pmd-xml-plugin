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

import com.google.common.io.Closeables;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PmdVersion {
  private static final String PROPERTIES_PATH = "/org/sonar/plugins/pmd/pmd-plugin.properties";

  private PmdVersion() {
    // Static utility class
  }

  public static String getVersion() {
    Properties properties = new Properties();

    InputStream input = null;
    try {
      input = PmdVersion.class.getResourceAsStream(PROPERTIES_PATH);
      properties.load(input);
    } catch (IOException e) {
      LoggerFactory.getLogger(PmdVersion.class).warn("Can not load the PMD version from the file " + PROPERTIES_PATH, e);
    } finally {
      Closeables.closeQuietly(input);
    }

    return properties.getProperty("pmd.version", "");
  }
}
