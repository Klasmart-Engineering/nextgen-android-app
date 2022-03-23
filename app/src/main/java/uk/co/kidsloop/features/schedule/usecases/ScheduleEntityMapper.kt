package uk.co.kidsloop.features.schedule.usecases

import javax.inject.Inject
import uk.co.kidsloop.features.schedule.network.response.schedule.ScheduleResponse

class ScheduleEntityMapper @Inject constructor() {

    fun toScheduleEntity(scheduleResponse: ScheduleResponse): ScheduleEntity {
        val entityList = mutableListOf<DataEntity>()
        for (data in scheduleResponse.data) {
            entityList.add(DataEntity(data.id, data.startAt, data.endAt, data.status, data.title, data.classType))
        }
        return ScheduleEntity(entityList, scheduleResponse.total)
    }
}
