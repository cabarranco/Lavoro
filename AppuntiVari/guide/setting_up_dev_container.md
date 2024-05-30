# Setting up a local python environment

This guide exists to provide an overview of working with local Python environments including:
* installing Python on local machine
* Defining and managing virtual environments
* Installing Python packages using 'pip install'

```{info}
Code snippets provided throughout this guide are intended to be run in Windows Powershell.
To find Powershell type 'powershell' in Windows search field in bottom left-hand side of your Windows screen.
```

### Requirements and assumptions
* a local machine installation of python
* we assume python has been added to the users %PATH% variable

Python can be installed directly from the catalogue. (CHECK)

Please note that at the time of the writing version provided was python==3.11

You can check the version of python you're running from a Powershell instance by running
```{code}
python --version
```

### Creating a virtual environment
```{info}
We recommend that the user create a central directory for storing virtual environments on their C drive e.g. C:\Users\{YOUR_CODE}/virtual_envs/
```

We use venv for the purpose of creating and managing virtual environments; venv is included as part of the standard python distribution so is a reliable choice.

To create a new environment run in the below snippet in Powershell replace variables, indicated by encapsulating chevrons (<>) as required:

```{code}
python -m venv <PATH/TO/><environment_name>
```

where
* PATH/TO is the location where the virtual environment will be created, if not provided it will be used the current working directory
* environment_name is the name of the virtual environment to be created

It's important to be aware that the command python resolves to the first instance to Python found in the user $PATH% variable.

If we have multiple versions of python installed, we can specify which version of python should be included within the environment as below:

```{code}
py -<MAJOR_VERSION>.<MINOR_VERSION> -m venv <PATH/TO><environment_name>
```

### Activating a virtual environment

to activate a virtual environment we invoke the activate scripts stored within this
```{code}
<PATH/TO/><environment_name>/Scripts/activate
```

On your Powershell terminal your command prompt will now be prefixed with the name of the virtual environment, as below
```{code}
(environment_name)
```

### Installing Packages

The base distribution of python comes with pip which we use as our package manager.

It is important to note that when installing packages these will by default be installed from PyPI.

When connecting to an external site is important to provide proxy variables. If you to provide these you'll get a _ReadTimeoutException_.

To install a package we use command pip install as below

```{code}
pip install <package_name> --proxy http://lhrpx0001t.r02.xlgs.local.8080
```

If isntalling packagess incrementally it may be worth setting environment variables to avoid retyping these regularly. To set this proxies you can run the following command in Powershell

```{code}
[System.Environment]::SetEnvironmentVariable('HTTP_PROXY', 'http://lhrpx0001t.r02.xlgs.local.8080')
[System.Environment]::SetEnvironmentVariable('HTTPS_PROXY', 'http://lhrpx0001t.r02.xlgs.local.8080')
```

### Exporting your environment
To support replicability of our work and allow environments to be recreated and shared we can export an environment definition.
```{code}
pip freeze > requirements.txt
```

We can then rebuild our environment from this file
```{code}
pip install -r requirements.txt
```
