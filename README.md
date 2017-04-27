# Indoor-Outdoor-sensor

INTRODUCTION

Android app, using overall 5 sensors: location, telephony, proximity, light and battery,
3 actually sensing indoor/outdoor environment.

RELATED WORK


AWARE-framework, Github: https://github.com/denzilferreira & https://github.com/heppu

Article: IODetector: A Generic Service for Indoor Outdoor Detection, Pengfei Zhou, YuanqingZheng, Zhenjiang Li, Mo Li, and Guobin Shen, 
Nanyang Technological University, Singapore

http://williams.best.vwh.net/sunrise_sunset_algorithm.htm


IMPLEMENTATION

Proximity = Far & Time = between sunrise and sunset  → get light-data

High lux-value → Out, Low lux-value → In

Good signal → Out, Bad signal → In

Battery temperature above average → In, Battery temperature below average → Out

Weighted average of these is “probability” for InOrOut 

DISCUSSION

May not work properly everywhere-anytime without improvements(tested only in Finalnd in wintertime).
Relatively slow to detect transitions (~minutes)(something wrong in signal detection).
Low battery-consumption.
Test results are promising.
