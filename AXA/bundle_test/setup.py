from setuptools import setup, find_packages

with open("requirements.txt", "r") as reqs_file:
  requirements - reqs_file.readlines()
  reqs_file.close()

setup(
  name="bundle_test",
  version="0.1.0",
  description="Logic required for running the pipeline in bundle_test",
  author="Bla Bla",
  author_email="bla.bla@dom.com",
  packages=find_packages(where="src"),
  package_dir={"": "src"},
  install_requires=requirements
)
