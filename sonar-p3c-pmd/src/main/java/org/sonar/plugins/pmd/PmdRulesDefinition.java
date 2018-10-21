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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.plugins.java.Java;
import org.sonar.plugins.xml.language.Xml;
import org.sonar.squidbridge.rules.ExternalDescriptionLoader;
import org.sonar.squidbridge.rules.PropertyFileLoader;
import org.sonar.squidbridge.rules.SqaleXmlLoader;

public final class PmdRulesDefinition implements RulesDefinition {

  public PmdRulesDefinition() {
    // do nothing
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(PmdConstants.REPOSITORY_KEY, Java.KEY)
      .setName(PmdConstants.REPOSITORY_NAME);

    extractRulesData(repository, "/org/sonar/plugins/pmd/rules.xml", "/org/sonar/l10n/pmd/rules/pmd");
    extractRulesData(repository, "/org/sonar/plugins/pmd/rules-p3c.xml", "/org/sonar/l10n/pmd/rules/pmd-p3c");

    repository.done();

	  NewRepository xmlRepository = context
			  .createRepository(PmdConstants.XML_REPOSITORY_KEY, Xml.KEY)
			  .setName(PmdConstants.XML_REPOSITORY_NAME);
	  extractRulesData(xmlRepository, "/org/sonar/plugins/pmd/rules-xml.xml", "/org/sonar/l10n/pmd/rules/pmd-xml");
	  xmlRepository.done();
  }

  static void extractRulesData(NewRepository repository, String xmlRulesFilePath, String htmlDescriptionFolder) {
    RulesDefinitionXmlLoader ruleLoader = new RulesDefinitionXmlLoader();
    ruleLoader.load(repository, PmdRulesDefinition.class.getResourceAsStream(xmlRulesFilePath), "UTF-8");
    ExternalDescriptionLoader.loadHtmlDescriptions(repository, htmlDescriptionFolder);
    PropertyFileLoader.loadNames(repository, PmdRulesDefinition.class.getResourceAsStream("/org/sonar/l10n/pmd.properties"));
    SqaleXmlLoader.load(repository, "/com/sonar/sqale/pmd-model.xml");
  }
}
