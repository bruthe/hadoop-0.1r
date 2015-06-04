@echo off
@rem The Hadoop command script 
@rem
@rem Environment Variables
@rem
@rem   JAVA_HOME        The java implementation to use.  Overrides JAVA_HOME.
@rem
@rem   HADOOP_HEAPSIZE  The maximum amount of heap to use, in MB. Default is 1000.
@rem
@rem   HADOOP_OPTS      Extra Java runtime options.
@rem
@rem   HADOOP_CONF_DIR  Alternate conf dir. Default is ${HADOOP_HOME}/conf.

:main
  setlocal enabledelayedexpansion
  
  set hadoop-command=%1
  shift
  if not defined hadoop-command (
      goto print_usage
  )
  
	call :make_command_arguments %*

  @rem some directories 
  set HADOOP_HOME=H:\job\springspace\hadoop_research\hadoop-0.1r
  set HADOOP_CONF_DIR="%HADOOP_HOME%"\conf
  
  if defined JAVA_HOME (
  	set JAVA_HOME="%JAVA_HOME%"
  	@echo run java in: %JAVA_HOME%
  ) else (
  	@echo Error: JAVA_HOME is not set.
  	exit /b %ERRORLEVEL%
  )
  
  set JAVA=%JAVA_HOME%\bin\java
  set JAVA_HEAP_MAX=-Xmx1000m 
  
  @rem check envvars which might override default args
  if defined HADOOP_HEAPSIZE (
  	@echo run with heapsize %HADOOP_HEAPSIZE%
  	set JAVA_HEAP_MAX="-Xmx""%HADOOP_HEAPSIZE%""m"
  	@echo %JAVA_HEAP_MAX%
  )
  
  @rem CLASSPATH initially contains $HADOOP_CONF_DIR
  SET CLASSPATH="%CLASSPATH%";%HADOOP_CONF_DIR%
  
  @rem for developers, add Hadoop classes to CLASSPATH
  if exist %HADOOP_HOME%\target\classes (
  	set CLASSPATH=%CLASSPATH%;%HADOOP_HOME%\target\classes
  )
  if exist %HADOOP_HOME%\target\test-classes (
  	set CLASSPATH=%CLASSPATH%;%HADOOP_HOME%\target\test-classes
  )
  
  @rem for releases, add hadoop jars & webapps to CLASSPATH
  if exist %HADOOP_HOME%\webapps (
    set CLASSPATH=%CLASSPATH%;$HADOOP_HOME
  )
  for %%f in ( %HADOOP_HOME%\hadoop-*.jar ) do (
    set CLASSPATH=!CLASSPATH!;%%f
  )
  
  @rem add libs to CLASSPATH
  for %%f in ( %HADOOP_HOME%\lib\*.jar ) do (
    set CLASSPATH=!CLASSPATH!;%%f
  )
  
  call :print_classpath
  @echo %hadoop-command-arguments%
  
  if %hadoop-command% == classpath (
    if not defined hadoop-command-arguments (
      @rem No need to bother starting up a JVM for this simple case.
      @echo %CLASSPATH%
      exit /b
    )
  )
  
  set commands=namenode datanode dfs fsck jobtracker tasktracker job jar 
  for %%c in ( %commands% ) do (
    if %hadoop-command% == %%c set commands=true  
  )
  if defined commands (
    call :%hadoop-command%
  ) else (
    set CLASSPATH=%CLASSPATH%;%CD%
    set CLASS=%hadoop-command%
  )
  
  call %JAVA% %JAVA_HEAP_MAX% %HADOOP_OPTS% -classpath %CLASSPATH% %CLASS% %hadoop-command-arguments%
  
  exit /b %ERRORLEVEL%
  
:namenode
  set CLASS=org.apache.hadoop.dfs.NameNode
  goto :eof
  
:datanode
  set CLASS=org.apache.hadoop.dfs.DataNode
  goto :eof
  
:dfs
  set CLASS=org.apache.hadoop.dfs.DFSShell
  goto :eof
  
:fsck
  set CLASS=org.apache.hadoop.dfs.DFSck
  goto :eof
  
:jobtracker
  set CLASS=org.apache.hadoop.mapred.JobTracker
  goto :eof
  
:tasktracker
  set CLASS=org.apache.hadoop.mapred.TaskTracker
  goto :eof
  
:jar
  set JAR=%1
  shift
  set CLASS=%0 org.apache.hadoop.util.PrintJarMainClass %JAR%
  
  if not errorlevel 0 do(
  	@echo Error: Could not find main class in jar file %JAR%
  	exit /b %ERRORLEVEL%
  )
  set CLASSPATH=%CLASSPATH%;%JAR%  
  goto :eof

:print_classpath
  @echo ------ final classpath is ------ 
  @echo,
  @echo %CLASSPATH%
  goto :eof

@rem This changes %1, %2 etc. Hence those cannot be used after calling this.
:make_command_arguments
  if "%1" == "--config" (
    shift
    shift
  )
  if [%2] == [] goto :eof
  shift
  set _arguments=
  :MakeCmdArgsLoop 
  if [%1]==[] goto :EndLoop 

  if not defined _arguments (
    set _arguments=%1
  ) else (
    set _arguments=!_arguments! %1
  )
  shift
  goto :MakeCmdArgsLoop 
  :EndLoop 
  set hadoop-command-arguments=%_arguments%
  goto :eof

:print_usage
  @echo Usage: hadoop COMMAND
  @echo where COMMAND is one of:
  @echo   namenode -format  format the DFS filesystem
  @echo   namenode          run the DFS namenode
  @echo   datanode          run a DFS datanode
  @echo   dfs               run a DFS admin client
  @echo   fsck              run a DFS filesystem checking utility
  @echo   jobtracker        run the MapReduce job Tracker node
  @echo   tasktracker       run a MapReduce task Tracker node
  @echo   job               manipulate MapReduce jobs
  @echo   jar ^<jar^>         run a jar file
  @echo  or
  @echo   CLASSNAME         run the class named CLASSNAME
  @echo Most commands print help when invoked w/o parameters.

endlocal