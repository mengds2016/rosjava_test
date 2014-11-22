wget http://www.udp.jp/software/nvxs-1.0.2.tar.gz;
wget http://www.udp.jp/software/Imager-AnimeFace-1.02.tar.gz;

tar xvzf nvxs-1.0.2.tar.gz;
cd nvxs-1.0.2;
./configure;
make;
sudo make install;

cd .. ;
cd Imager-AnimeFace-1.02;
tar xvzf Imager-AnimeFace-1.02.tar.gz;
sudo cpan Imager; ## install Imager
perl Makefile.PL ;
make;
sudo make install;

## export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
