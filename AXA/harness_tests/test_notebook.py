# Databricks notebook source
# Macic %pip install /dbfs/FileStore/Utils/Utils-0.1-py3-none-any.whl

# COMMAND ----------

msg = """
name, num, num_fact"""
df = pd.DataFrame(['a', 1), ('b', 2)], columns=['name', 'num'])
df = messy_funct(df)
for i in range(len(df)):
  msg += f"""
  {i}, {df.iloc[i]['name']}, {df.iloc[i]['num']}, {df.iloc[i]['num_fact']}"""

# COMMAND ----------

dbutils.notebook.exit(msg)
