from setuptools import setup, find_packages

setup(
  name="{{cookiecutter.repo_name}}",
  author="{{cookiecutter.author}}",
  author_email="{{cookiecutter.author_email}}",
  package=find_packages(where="src"),
  package_dir={ "": "src" },
  version="{{cookiecutter.version}}"
)
