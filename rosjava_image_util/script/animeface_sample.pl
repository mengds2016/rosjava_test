#!/usr/local/bin/perl

use lib qw(
        /home/udp/local/lib/perl5/site_perl/5.8.8/mach
        /home/udp/local/lib/perl5/site_perl/5.8.8
);

use Imager;
use Imager::AnimeFace;
use strict;
use warnings;
use Data::Dumper;

my $im = Imager->new();
my @results;

my $inpath = "./test.jpg";
if ( @ARGV > 0 ){
    $inpath = $ARGV[0];
}

my $outpath = "./out.jpg";
if ( @ARGV > 1 ){
    $outpath = $ARGV[1];
}

$im->read(file=>$inpath);

my $results = detect_animeface($im);
my $blue = Imager::Color->new(0, 0, 255);
my $red = Imager::Color->new(255, 0, 0);

foreach my $face (@{$results}) {
    print ":face $face->{face}->{x} $face->{face}->{y} $face->{face}->{width} $face->{face}->{height}\n";
    $im->box(
	xmin => $face->{face}->{x},
	ymin => $face->{face}->{y},
	xmax => $face->{face}->{x} + $face->{face}->{width},
	ymax => $face->{face}->{y} + $face->{face}->{height},
	color => $blue
    );
    print ":eye :right $face->{eyes}->{left}->{x} $face->{eyes}->{left}->{y} $face->{eyes}->{left}->{width} $face->{eyes}->{left}->{height}\n";
    $im->box(
	xmin => $face->{eyes}->{left}->{x},
	ymin => $face->{eyes}->{left}->{y},
	xmax => $face->{eyes}->{left}->{x} + $face->{eyes}->{left}->{width},
	ymax => $face->{eyes}->{left}->{y} + $face->{eyes}->{left}->{height},
	color => $red
    );
    print ":eye :left $face->{eyes}->{right}->{x} $face->{eyes}->{right}->{y} $face->{eyes}->{right}->{width} $face->{eyes}->{right}->{height}\n";
    $im->box(
	xmin => $face->{eyes}->{right}->{x},
	ymin => $face->{eyes}->{right}->{y},
	xmax => $face->{eyes}->{right}->{x} + $face->{eyes}->{right}->{width},
	ymax => $face->{eyes}->{right}->{y} + $face->{eyes}->{right}->{height},
	color => $red
    );
    last;
}

$im->write(file => $outpath, type=>'jpeg');
