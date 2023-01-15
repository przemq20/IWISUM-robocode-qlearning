import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import os

columns = ["round", "reward"]
# assign directory
directory = 'out/production/RobocodeQlearning/QLearner.data'

# iterate over files in
# that directory
for filename in os.listdir(directory):
    f = os.path.join(directory, filename)
    # checking if it is a file
    if os.path.isfile(f) and os.path.splitext(f)[1] == '.csv':
        print(f)
        name = (os.path.basename(f).split('/')[-1])
        args = (f).split('_')
        alpha = args[1]
        gamma = args[2]
        epsilon = args[3][:len(args[3]) -4]
        df = pd.read_csv(f,usecols = columns)

        running = df['reward'].rolling(50).mean()
        plt.title('Alpha= ' + alpha + ", gamma= " + gamma + ', epsilon= ' + epsilon)
        plt.xlabel('Round')
        plt.ylabel('Reward')
        plt.plot(df['round'], df['reward'], running)
        # plt.show()
        plt.savefig(name[:len(name) - 4] + '.png')
        plt.clf()


