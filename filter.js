//打印日志
utils.log("注意：你已开启座位过滤")
//创建过滤后的数组
let newSeats = []
//循环过滤
for (let i = 0; i < seats.length; i++) {
    //获取每个座位
    let seat = seats[i]
    //获取当前座位区域名称
    let areaName = seat.getArea().getName()
    //获取当前座位座位号
    let id = seat.getName()
    //进行过滤
    if (areaName == '九楼阅览室' || areaName == '十楼阅览室') {
        //九楼十楼不过滤
        newSeats.push(seat)
    } else if (areaName == '六楼阅览室') {
        //六楼座位号小于52时仅选择座位号除4余数为2的座位
        if (id < 52) {
            if (id % 4 == 2) {
                newSeats.push(seat)
            }
        }
        //六楼座位号大于64小于120不过滤
        if (id > 64 && id < 120) {
            newSeats.push(seat)
        }
    } else {
        //其他楼层仅选择座位号除4余数为2的座位
        if (id % 4 == 2) {
            newSeats.push(seat)
        }
    }
}
//返回过滤后的座位
newSeats