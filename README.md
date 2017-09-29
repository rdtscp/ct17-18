# Deadlines #

1. [Part 1 (parser)](desc/part1/), Thursday 12 October 2017  at 4pm, weight = 20%
2. Part 2 (ast builder + semantic analyser), Thursday 26 October 2017  at 4pm, weight = 20%
3. Part 3 (code generator), Thursday 16 November 2017 at 4pm, weight = 30%
4. Part 4 (LLVM-based compiler pass), Monday 15 January 2018, 10am, weight = 30%

Note that specific instructions for each part can be found above by clicking on the part name.

# Scoreboard #

We automatically run a series of hidden test programs using your compiler about once a day.
You can keep track of your progress and see how many tests pass/fail using the scoreboard by following this link:
https://htmlpreview.github.io/?https://bitbucket.org/cdubach/ct-17-18/raw/master/scoreboard/scoreboard.html

# Marking #

The marking will be done using an automated test suite on a dice machine using Java 8 (1.8 runtime).
Please note that you are not allowed to modify the `Main.java` file which is the main entry point to the compiler.
A checksum on the file will be performed to ensure the file has not be tempered with.
Also make sure that the build script provided remains unchanged so that your project can be built on dice.
Furthermore, you may not use any external libraries.

For parts 1-3 of the coursework, the marking will be a function of the number of successful tests as shown in the scoreboard and a series of hidden tests.

## Part 1-2
66% of the mark will be determined by the scoreboard tests and 33% will be determined by the hidden tests.
You will get one point for each passing test and -1 for each failing test.
Then the mark is calculated by dividing the number of points achieved by the number of tests.

## Part 3
66% of the mark will be determined by the visible scoreboard tests and 33% will be determined by the hidden tests.
The mark will directly be proportial to the number of passed tests (no negative point).
So for instance, passing 7 out of 50 tests would result in a mark of 14/100.


# Setup #

## Bitbucket ##
We will rely on bitbucket and it is mandatory to use it for this coursework.
Bitbucket is an online repository that can be used with the git control revision system.

Your first task should be to setup a bitbucket account using your university email address.
You should then send register your bitbucket id with your university id using this [Google form](https://docs.google.com/forms/d/1z2EthflazoU2bvfnJlrCWB_-AqB4ZxIgsJW-8SWiXyM) so that we can run the automated test suite on your repository.
Details on how to fork the ct-17-18 repository are given below.
Important: do not share your code and repository with anyone and keep your source code secret.
If we identify that two students have identical portion of code, both will be considered to have cheated.

## Development environment (editor)
You can choose to use a development environment for your project. DICE machines have the following integrated development environments (IDE) for Java installed:

* Community edition of [IntelliJ](https://www.jetbrains.com/idea/).
* [Eclipse](https://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/marsr) for Java.

Alternatively, you can use Emacs, vim, or your favourite text editor. Choose whichever you are confident with.

## Obtaining your own copy of the ct-17-18 repository 
We are going to be using the Git revision control system during the course. Git is installed on DICE machines. If you use your own machine then make sure to install Git.

You will need to have your own copy of the ct-17-18 repository. In order to fork this repository, click on the plus sign in the left panel as shown below and click on *Fork* this repository:

![Forking the CT-17-18 repository](/figures/bb_menu.png "Forking this repository.")

![Forking the CT-17-18 repository](/figures/bb_fork.png "Forking this repository.")

Here you can name your repository and give it an optional description. **Remember** to tick "This is a private repository" as shown below:
 
![Forking the CT-17-18 repository](/figures/bb_fork_private.png "Forking this repository.")

Finally, click "Fork repository" to finish. After forking you should grant the teaching staff read access to your repository. Click on Settings (the gear icon), and then go to "Access management", the window should look similar to the figure below:

![Granting the teaching staff read access](/figures/repopermissions.png "Granting the teaching staff read access.")

You should grant the following users *write* access:

* Christophe Dubach (username: cdubach)
* Federico Pizzuti (username: fedepiz)
* Adam Harries (username: AdamHarries)

Next, you will have to clone the forked repository to your local machine. In order to clone the repository you should launch a terminal and type:
```
$ git clone https://YOUR-USERNAME@bitbucket.org/YOUR-USERNAME/YOUR-REPOSITORY-NAME
```
where `YOUR-USERNAME` must be *your* Bitbucket account name, and `YOUR-REPOSITORY-NAME` must be the name you chose for your fork of the ct-17-18 repository.


## Building the ct-17-18 project
In order to build the project you must have Ant installed. On DICE machines Ant is already installed.
Your local copy of the ct-17-18 repository contains an Ant build file (`build.xml`).
If you are using an IDE, then you can import the build file.
Otherwise, you can build the project from the commandline by typing:
```
$ ant build
```
This command outputs your compiler in a directory called `bin` within the project structure. Thereafter, you can run your compiler from the commandline by typing:
```
$ java -cp bin Main
```
The parameter `cp` instructs the Java Runtime to include the local directory `bin` when it looks for class files.

You can find a series of tests in the `tests` folder. To run the lexer on one of them, you can type:

```
$ java -cp bin Main -lexer tests/fibonacci.c
```


You can clean the `bin` directory by typing:
```
$ ant clean
```
This command effectively deletes the `bin` directory.

## Working with git and pushing your changes

Since we are using an automated marking mechnism (based on how many progams can run successfully through your compiler), it is important to understand how git works. If you want to benefit from the nightly automatic marking feedback, please ensure that you push all your changes daily onto your bitbucket centralised repository.

We suggest you follow the excelent [tutorial](https://www.atlassian.com/git/tutorials/what-is-version-control) from atlassian on how to use git. In particular you will need to understand the following basic meachnisms:

* [add and commit](https://www.atlassian.com/git/tutorials/saving-changes)
* [push](https://www.atlassian.com/git/tutorials/syncing/git-push)
