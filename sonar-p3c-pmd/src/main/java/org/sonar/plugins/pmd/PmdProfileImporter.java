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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.java.Java;
import org.sonar.plugins.pmd.xml.PmdProperty;
import org.sonar.plugins.pmd.xml.PmdRule;
import org.sonar.plugins.pmd.xml.PmdRuleset;

import javax.annotation.Nullable;

import java.io.Reader;
import java.util.List;

public class PmdProfileImporter extends ProfileImporter {

  private final RuleFinder ruleFinder;
  private static final Logger LOG = LoggerFactory.getLogger(PmdProfileImporter.class);

  public PmdProfileImporter(RuleFinder ruleFinder) {
    super(PmdConstants.REPOSITORY_KEY, PmdConstants.PLUGIN_NAME);
    setSupportedLanguages(Java.KEY);
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile importProfile(Reader pmdConfigurationFile, ValidationMessages messages) {
    PmdRuleset pmdRuleset = parsePmdRuleset(pmdConfigurationFile, messages);
    return createRuleProfile(pmdRuleset, messages);
  }

  protected RulesProfile createRuleProfile(PmdRuleset pmdRuleset, ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create();
    for (PmdRule pmdRule : pmdRuleset.getPmdRules()) {
      String ruleClassName = pmdRule.getClazz();
      if (PmdConstants.XPATH_CLASS.equals(ruleClassName)) {
        messages.addWarningText("PMD XPath rule '" + pmdRule.getName()
          + "' can't be imported automatically. The rule must be created manually through the SonarQube web interface.");
      } else {
        String ruleRef = pmdRule.getRef();
        if (ruleRef == null) {
          messages.addWarningText("A PMD rule without 'ref' attribute can't be imported. see '" + ruleClassName + "'");
        } else {
          Rule rule = ruleFinder.find(RuleQuery.create().withRepositoryKey(PmdConstants.REPOSITORY_KEY).withConfigKey(ruleRef));
          if (rule != null) {
            ActiveRule activeRule = profile.activateRule(rule, PmdLevelUtils.fromLevel(pmdRule.getPriority()));
            setParameters(activeRule, pmdRule, rule, messages);
          } else {
            messages.addWarningText("Unable to import unknown PMD rule '" + ruleRef + "'");
          }
        }
      }
    }
    return profile;
  }

  private static void setParameters(ActiveRule activeRule, PmdRule pmdRule, Rule rule, ValidationMessages messages) {
    for (PmdProperty prop : pmdRule.getProperties()) {
      String paramName = prop.getName();
      if (rule.getParam(paramName) == null) {
        messages.addWarningText("The property '" + paramName + "' is not supported in the pmd rule: " + pmdRule.getRef());
      } else {
        activeRule.setParameter(paramName, prop.getValue());
      }
    }
  }

  protected PmdRuleset parsePmdRuleset(Reader pmdConfigurationFile, ValidationMessages messages) {
    try {
      SAXBuilder parser = new SAXBuilder();
      Document dom = parser.build(pmdConfigurationFile);
      Element eltResultset = dom.getRootElement();
      Namespace namespace = eltResultset.getNamespace();
      PmdRuleset pmdResultset = new PmdRuleset();
      for (Element eltRule : getChildren(eltResultset, "rule", namespace)) {
        PmdRule pmdRule = new PmdRule(eltRule.getAttributeValue("ref"));
        pmdRule.setClazz(eltRule.getAttributeValue("class"));
        pmdRule.setName(eltRule.getAttributeValue("name"));
        pmdRule.setMessage(eltRule.getAttributeValue("message"));
        parsePmdPriority(eltRule, pmdRule, namespace);
        parsePmdProperties(eltRule, pmdRule, namespace);
        pmdResultset.addRule(pmdRule);
      }
      return pmdResultset;
    } catch (Exception e) {
      String errorMessage = "The PMD configuration file is not valid";
      messages.addErrorText(errorMessage + " : " + e.getMessage());
      LOG.error(errorMessage, e);
      return new PmdRuleset();
    }
  }

  private static List<Element> getChildren(Element parent, String childName, @Nullable Namespace namespace) {
    if (namespace == null) {
      return parent.getChildren(childName);
    } else {
      return parent.getChildren(childName, namespace);
    }
  }

  private static void parsePmdProperties(Element eltRule, PmdRule pmdRule, @Nullable Namespace namespace) {
    for (Element eltProperties : getChildren(eltRule, "properties", namespace)) {
      for (Element eltProperty : getChildren(eltProperties, "property", namespace)) {
        pmdRule.addProperty(new PmdProperty(eltProperty.getAttributeValue("name"), eltProperty.getAttributeValue("value")));
      }
    }
  }

  private static void parsePmdPriority(Element eltRule, PmdRule pmdRule, @Nullable Namespace namespace) {
    for (Element eltPriority : getChildren(eltRule, "priority", namespace)) {
      pmdRule.setPriority(eltPriority.getValue());
    }
  }
}
