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

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plugins.pmd.xml.PmdRuleset;

import java.io.Reader;
import java.io.StringReader;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PmdProfileImporterTest {
  PmdProfileImporter importer;
  ValidationMessages messages;

  @Before
  public void setUpImporter() {
    messages = ValidationMessages.create();
    importer = new PmdProfileImporter(createRuleFinder());
  }

  @Test
  public void should_import_pmd_ruleset() {
    Reader reader = read("/org/sonar/plugins/pmd/simple.xml");

    PmdRuleset pmdRuleset = importer.parsePmdRuleset(reader, messages);

    assertThat(pmdRuleset.getPmdRules()).hasSize(4);
  }

  @Test
  public void should_import_simple_profile() {
    Reader reader = read("/org/sonar/plugins/pmd/simple.xml");

    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(profile.getActiveRules()).hasSize(3);
    assertThat(profile.getActiveRuleByConfigKey("pmd", "rulesets/java/coupling.xml/ExcessiveImports")).isNotNull();
    assertThat(profile.getActiveRuleByConfigKey("pmd", "rulesets/java/design.xml/UseNotifyAllInsteadOfNotify")).isNotNull();
    assertThat(messages.hasErrors()).isFalse();
  }

  @Test
  public void should_import_profile_with_xpath_rule() {
    Reader reader = read("/org/sonar/plugins/pmd/export_xpath_rules.xml");

    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(profile.getActiveRules()).isEmpty();
    assertThat(messages.hasWarnings()).isTrue();
  }

  @Test
  public void should_import_parameter() {
    Reader reader = read("/org/sonar/plugins/pmd/simple.xml");

    RulesProfile profile = importer.importProfile(reader, messages);
    ActiveRule activeRule = profile.getActiveRuleByConfigKey("pmd", "rulesets/java/coupling.xml/ExcessiveImports");

    assertThat(activeRule.getParameter("minimum")).isEqualTo("30");
  }

  @Test
  public void should_import_default_priority() {
    Reader reader = read("/org/sonar/plugins/pmd/simple.xml");

    RulesProfile profile = importer.importProfile(reader, messages);
    ActiveRule activeRule = profile.getActiveRuleByConfigKey("pmd", "rulesets/java/coupling.xml/ExcessiveImports");

    assertThat(activeRule.getSeverity()).isSameAs(RulePriority.BLOCKER);
  }

  @Test
  public void should_import_priority() {
    Reader reader = read("/org/sonar/plugins/pmd/simple.xml");

    RulesProfile profile = importer.importProfile(reader, messages);

    ActiveRule activeRule = profile.getActiveRuleByConfigKey("pmd", "rulesets/java/design.xml/UseNotifyAllInsteadOfNotify");
    assertThat(activeRule.getSeverity()).isSameAs(RulePriority.MINOR);

    activeRule = profile.getActiveRuleByConfigKey("pmd", "rulesets/java/coupling.xml/CouplingBetweenObjects");
    assertThat(activeRule.getSeverity()).isSameAs(RulePriority.CRITICAL);
  }

  @Test
  public void should_import_pmd_configuration_with_unknown_nodes() {
    Reader reader = read("/org/sonar/plugins/pmd/complex-with-unknown-nodes.xml");

    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(profile.getActiveRules()).hasSize(3);
  }

  @Test
  public void should_deal_with_unsupported_property() {
    Reader reader = read("/org/sonar/plugins/pmd/simple.xml");

    RulesProfile profile = importer.importProfile(reader, messages);
    ActiveRule check = profile.getActiveRuleByConfigKey("pmd", "rulesets/java/coupling.xml/CouplingBetweenObjects");

    assertThat(check.getParameter("threshold")).isNull();
    assertThat(messages.getWarnings()).hasSize(2);
  }

  @Test
  public void should_fail_on_invalid_xml() {
    Reader reader = new StringReader("not xml");

    importer.importProfile(reader, messages);

    assertThat(messages.getErrors()).hasSize(1);
  }

  @Test
  public void should_warn_on_unknown_rule() {
    Reader reader = read("/org/sonar/plugins/pmd/simple.xml");

    importer = new PmdProfileImporter(mock(RuleFinder.class));
    RulesProfile profile = importer.importProfile(reader, messages);

    assertThat(profile.getActiveRules()).isEmpty();
    assertThat(messages.getWarnings()).hasSize(4);
  }

  static Reader read(String path) {
    return new StringReader(PmdTestUtils.getResourceContent(path));
  }

  static RuleFinder createRuleFinder() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.find(any(RuleQuery.class))).then(new Answer<Rule>() {
      @Override
      public Rule answer(InvocationOnMock invocation) {
        RuleQuery query = (RuleQuery) invocation.getArguments()[0];
        String configKey = query.getConfigKey();
        String key = configKey.substring(configKey.lastIndexOf('/') + 1, configKey.length());
        Rule rule = Rule.create(query.getRepositoryKey(), key, "").setConfigKey(configKey).setSeverity(RulePriority.BLOCKER);
        if (rule.getConfigKey().equals("rulesets/java/coupling.xml/ExcessiveImports")) {
          rule.createParameter("minimum");
        }
        return rule;
      }
    });
    return ruleFinder;
  }
}
