[![ACTUS](https://github.com/actusfrf/actus-resources/blob/master/logos/actus_logo.jpg "ACTUS Financial Research Foundation")](https://www.actusfrf.org)

actus-core
=======

Project ACTUS develops and maintains a number of Java libraries (the actus libraries) as part of its algorithmic standards.

This repository contains the source code of the actus-core library, project ACTUS' core library implementing the Contract Types.

The actus libraries will be released as Free and Open Source Software. 

Documentation
-------------

Documentation for Project ACTUS can be found at https://www.actusfrf.org.

Documentation for Project ACTUS' various API's will be made public soon.

Building actus-core
-------------

The source code can be cloned using [git](http://git-scm.com/) from GitHub:
```
  git clone HTTPS https://github.com/projectactus/actus-core.git

```

The actus libraries use  [Apache Maven](http://maven.apache.org/) as the build system.
Version 3.2.0 or later is required.
Simply run this command to compile and install the source code locally:

```
  mvn install
```

Note that actus libraries are based on Java SE 8.
Version 8u40 or later is required to compile the code.

Status
------

The actus libraries are a result of several years of research and development 
work. The current architecture is a result of multiple, complete redesigns
aiming at improving quality and integrability. At this point, we are confident
that the API to the actus libraries will remain "stable" going forward. 

Status Update 8 Dec 2023 
-------------------------
This is version 1.0.1 of actus-core 
It implements actus-dictionary version 1.4 
New features in this version of actus-core:
   *  new contract type BoundaryControlledSwitch (BCS) is supported ( issue #246)
   *  variable UMP contracts are supported ( issue #245 )
   *  non integer Quantity is supported on COM and STK contracts ( issue #244 )    
There are no known compatibility issues with earlier versions of actus-core or with
contract definitions supported by earlier versions of actus-core.

Status Update 6 June 2024 
-------------------------
Goal of this branch is to have a version of actus-core Version 1.0.1 which has its build scripts 
modified to work with openjdk17 and more current maven and gradle. Then this could be included 
in a branch of actus-webapp which includes an initial Risk Factor 2.0 API 

actus libraries
--------------

The actus libraries are formed by a number of components:

* [actus-core](https://github.com/actusfrf/actus-core#actus-core/README.md)
* [actus-testsuite](https://github.com/actusfrf/actus-testsuite#actus-testsuite/README.md)


Questions and feedback
----------------------

Consult our [homepage](https://www.actusfrf.org) for general information. 

Questions should be directed to info@actusfrf.org.

Bugs can be reported as a GitHub issue at
https://github.com/actusfrf/actus-core/issues.

Contributing
------------

Glad you are considering to contribute. Project ACTUS lives from 
contributions from an motivated developers community.

While we welcome any contribution we require developers to follow a
certain protocol. You may find more information on our 
[homepage](https://www.actusfrf.org).

Contributions to the actus library can generally be made through pull 
requests on GitHub. Therefore, the first thing you should do is to get
a GitHub account if you don't have it already. Then, clone the repository
to which you want to contribute using the "Fork" button in the upper 
right corner of our GitHub repository page. Finally, check out your clone 
to your machine, start coding, push your changes to your clone and submit 
a pull request to our repository.

Once you submitted a pull request the ACTUS core-team will have a look at
your code and will decide whether we can accept right away, require 
additional changes, or whether the changes a rejected. Since ACTUS really
aims at developing and promoting an industry-wide standard, the review
process may take a while. Please don't be discouraged but hang-on.

