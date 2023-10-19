#!/bin/bash

#!/bin/sh
#SBATCH --t 90:00:00
#SBATCH --account naiss2023-22-1043
#SBATCH --N 1
#SBATCH --C fat
#SBATCH --exclusive
#SBATCH --mail-user=elis.carlberg.larsson@liu.se
#SBATCH --mail-type=ALL


# choose java version
ml Java/1.8.0_74-nsc1


java -jar -Xmx110g -Xms110g target/PCPLDA-9.2.2.jar --run_cfg=/proj/textsoc/users/x_elcar/PartiallyCollapsedLDA/src/main/resources/configuration/us_congress.cfg