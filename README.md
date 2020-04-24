# sonar-pmd-xml-plugin
Based on sonar-pmd-plugin, a useful example to add a pmd-xml rule.   
Acknowledge @mrprince's contribution on integrated alibaba p3c.  
***
## UPDATE  
2020/4/24
订正PmdTemplate里的多余类。
2018/11/19
在使用xml和java的sonar profile同时扫描时,会出现部分解析错误,修复了PmdExcutor.executePmd,将java和xml的parse区分开;  
fix the bug of parseException when PmdExcutor parsing xml with java parser.  
## Usage  
**How to use**:  
```
git clone https://github.com/phinehasz/sonar-pmd-xml-plugin
mvn package
put this jar in your sonar{version}\extensions\plugins 
restart sonar
```
**How to add your custom pmd-xml rule**:  
Based on my plugin, you need to configure 4 files.  
`/org/sonar/plugins/pmd/rules.xml`  
`/org/sonar/l10n/pmd/rules/pmd`  
`/com/sonar/sqale/pmd-model.xml`  
`/org/sonar/l10n/pmd.properties`  
just copy like what I have done    :)  
***
## What I have done  
I modified some codes so the original sonar-pmd-plugin can support configure pmd-xml rule now!  
***
## Chinese Tutorial
**需求**  
sonar-pmd插件只有添加了pmd的java规则,现在需要添加pmd的xml规则,更准确是添加自定义的xml规则.  

**步骤**:  
为了更好集成和示范,选择前人已集成p3c的sonar-pmd插件.  
url: `https://github.com/mrprince/sonar-p3c-pmd`  
`git clone` 到本地  
集成分为两个环节:  
1.规则配置  
2.源码修改  


**规则配置**  
该插件首先依赖`PmdRulesDefinition`对仓库`repository`进行定义,从
```
extractRulesData(repository, "/org/sonar/plugins/pmd/rules.xml", "/org/sonar/l10n/pmd/rules/pmd");
```
方法内部,可以得知其是读取外部配置来初始化pmd的rules.  
一共有四处需要配置:  
`/org/sonar/plugins/pmd/rules.xml`  
`/org/sonar/l10n/pmd/rules/pmd`  
`/com/sonar/sqale/pmd-model.xml`  
`/org/sonar/l10n/pmd.properties`  
对于配置,没有什么多说的,原则就是模仿!  
`l10n`下的html是需要和rule的key一致.作用是sonar的rule页面展示. 
`pmd.properties`的`rule.pmd-xml.MistypedCDATASection.name`中,rule.`pmd-xml`代表repository名字,需要一致.  

**源码修改**  
在`PmdRulesDefinition`类的`define(Context context)`里可以看到`extractRulesData`是读取配置信息,如果想分别管理不同类型的规则,例如pmd原生和p3c规则,就可以分别配置,另外读取.  
`NewRepository`类是和sonar的规则语言绑定的,所以另外增加一个新的repository对象,添加`Xml.Key`  
`pom.xml` 新增依赖:  
```
<dependency>
          <groupId>org.sonarsource.sonar-xml-plugin</groupId>
          <artifactId>sonar-xml-plugin</artifactId>
          <version>1.3</version>
          <scope>provided</scope>
</dependency>
```
```
NewRepository xmlRepository = context
			  .createRepository(PmdConstants.XML_REPOSITORY_KEY, Xml.KEY)
			  .setName(PmdConstants.XML_REPOSITORY_NAME);
```
模仿之前的repository设置,将一些常量写在`PmdConstants`类里,后续还会经常用到这个string值.  

入口:`PmdSensor`  
真正的入口是`PmdSensor`,重写父类`Sensor`的`analyse`方法,定位到这个方法,继续开始修改.  
因为添加了一个新的仓库,而且从源码得知,一开始代码只是支持java的规则,全部都是写死的,现在需要新增xml.  修改该类的`shouldExecuteOnProject`和`hasFilesToCheck`,这里显然也是和sonar接口对接的方法,用于判断是否执行的.  
```
@Override
  public boolean shouldExecuteOnProject(Project project) {
    return (hasFilesToCheck(Type.MAIN, PmdConstants.REPOSITORY_KEY))
      || (hasFilesToCheck(Type.TEST, PmdConstants.TEST_REPOSITORY_KEY))
			|| (hasFilesToCheck(Type.MAIN, PmdConstants.XML_REPOSITORY_KEY)) ;
  }

  private boolean hasFilesToCheck(Type type, String repositoryKey) {
    FilePredicates predicates = fs.predicates();
    Iterable<File> files = fs.files(predicates.or(
    		predicates.and(predicates.hasLanguage(Xml.KEY),predicates.hasType(type)),
			predicates.and(predicates.hasLanguage(Java.KEY),
      predicates.hasType(type))));
    return !Iterables.isEmpty(files) && !profile.getActiveRulesByRepository(repositoryKey).isEmpty();
  }
```
新增了xml的判断,`Type.MAIN`是代表扫描的source是在src/main下,和src/test对应.`FilePredicates`很像之前接触的`FileFilter`类,使用方式也很像,为了有xml或java时都返回true,就在外面写一个`predicates.or`,这种类的设计感觉除了写着麻烦,实际上很好理解.  
接下来从`analyse`方法一步步进去,遇到有硬编码和判断语言类型的地方就着手修改.  

`PmdExecutor` 执行pmd的地方.  
```
private Report executePmd(URLClassLoader classLoader) {
    ...
    PmdTemplate pmdFactory = createPmdTemplate(classLoader);
    executeRules(pmdFactory, context, javaFiles(Type.MAIN), PmdConstants.REPOSITORY_KEY);
     ...
    return report;
  }
 ```
可以看到,是通过PmdTemplate来执行rules,模仿它新增一句:  
`executeRules(pmdFactory, context, xmlFiles(Type.MAIN), PmdConstants.XML_REPOSITORY_KEY);`  
并且提供自己的`xmlFiles`方法,基于语言过滤.  
为了一探究竟,顺便看看`PmdTemplate`类  
从代码看是通过`create`方法初始化`PMDConfiguration`和`SourceCodeProcessor`两个对象.`SourceCodeProcessor`是传入`InputStream`和`rulesets`来分析每个文件的PMD核心分析类.  
通过`languageVersions`来控制可使用何种语言的rule.  
原先是只添加了java的LanguageModule,现在需要增加xml部分.对pmd源码进行分析,即可知道`new XmlLanguageModule().getVersion("")`可获得xml的`languageVersion`.   

`PmdViolationRecorder` 输出  
最后输出的地方仍旧需要修改.
```
private Rule findRuleFor(RuleViolation violation) {
    String ruleKey = violation.getRule().getName();
    Rule xmlRule = ruleFinder.findByKey(PmdConstants.XML_REPOSITORY_KEY, ruleKey);
	  if (xmlRule != null) {
		  return xmlRule;
	  }
	  Rule rule = ruleFinder.findByKey(PmdConstants.REPOSITORY_KEY, ruleKey);
	  if (rule != null) {
		  return rule;
	  }
	  return ruleFinder.findByKey(PmdConstants.TEST_REPOSITORY_KEY, ruleKey);
  }
  ```
这里需要添加 xmlRule,否则是找一个普通javaRule,没有则返回java的testRule  

源码改的差不多了.但test里的类还需要修改,如果直接执行,会有部分类报错,因为它test里也是硬编码了java的.当然也可以选择`-Dmaven.test.skip=true`.

最快捷的办法,下载我的代码,在其基础上新增配置.  
```
git clone https://github.com/phinehasz/sonar-pmd-xml-plugin
mvn package
放到sonar{version}\extensions\plugins 
restart sonar即可
```
