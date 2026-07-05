// zuerst den Server mit command-b booten


{ Pan2.ar(SinOsc.ar(440, 0, 0.2), -1) }.play;  // sollte NUR rechts zu hören sein

{ Pan2.ar(SinOsc.ar(640, 0, 0.2), 1) }.play;  // sollte NUR rechts zu hören sein


{ Balance2.ar(SinOsc.ar(640), SinOsc.ar(440), -1) }.play


{ Balance2.ar(SinOsc.ar(440), SinOsc.ar(1000), Line.kr(-1, 1, 5)) }.play
