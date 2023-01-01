import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

columns = ["round", "reward"]

df = pd.read_csv("out/production/RobocodeQlearning/QLearner.data/data.csv",usecols = columns)

running = df['reward'].rolling(10).mean()
plt.xlabel('Round')
plt.ylabel('Reward')
plt.plot(df['round'], df['reward'], running)
plt.show()
