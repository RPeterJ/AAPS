package info.nightscout.androidaps.db

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.*
import info.nightscout.androidaps.events.*
import info.nightscout.shared.logging.AAPSLogger
import info.nightscout.shared.logging.LTag
import info.nightscout.androidaps.plugins.bus.RxBus
import info.nightscout.androidaps.plugins.iob.iobCobCalculator.events.EventNewHistoryData
import io.reactivex.rxjava3.disposables.Disposable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompatDBHelper @Inject constructor(
    val aapsLogger: AAPSLogger,
    val repository: AppRepository,
    val rxBus: RxBus
) {

    fun dbChangeDisposable(): Disposable = repository
        .changeObservable()
        .doOnSubscribe {
            rxBus.send(EventNewBG(null))
        }
        .subscribe {
            /**
             * GlucoseValues can come in batch
             * oldest one should be used for invalidation, newest one for for triggering Loop.
             * Thus we need to collect both
             *
             */
            var newestGlucoseValue: GlucoseValue? = null
            it.filterIsInstance<GlucoseValue>().maxByOrNull { gv -> gv.timestamp }?.let { gv ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventNewBg $gv")
                rxBus.send(EventNewBG(gv))
                newestGlucoseValue = gv
            }
            it.filterIsInstance<GlucoseValue>().minOfOrNull { gv -> gv.timestamp }?.let { timestamp ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventNewHistoryData $timestamp $newestGlucoseValue")
                rxBus.send(EventNewHistoryData(timestamp, true, newestGlucoseValue))
            }
            it.filterIsInstance<Carbs>().minOfOrNull { t -> t.timestamp }?.let { timestamp ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventTreatmentChange $timestamp")
                rxBus.send(EventTreatmentChange())
                rxBus.send(EventNewHistoryData(timestamp, false))
            }
            it.filterIsInstance<Bolus>().minOfOrNull { t -> t.timestamp }?.let { timestamp ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventTreatmentChange $timestamp")
                rxBus.send(EventTreatmentChange())
                rxBus.send(EventNewHistoryData(timestamp, false))
            }
            it.filterIsInstance<TemporaryBasal>().minOfOrNull { t -> t.timestamp }?.let { timestamp ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventTempBasalChange $timestamp")
                rxBus.send(EventTempBasalChange())
                rxBus.send(EventNewHistoryData(timestamp, false))
            }
            it.filterIsInstance<ExtendedBolus>().minOfOrNull { t -> t.timestamp }?.let { timestamp ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventExtendedBolusChange $timestamp")
                rxBus.send(EventExtendedBolusChange())
                rxBus.send(EventNewHistoryData(timestamp, false))
            }
            it.filterIsInstance<EffectiveProfileSwitch>().firstOrNull()?.let { eps ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventEffectiveProfileSwitchChanged $eps")
                rxBus.send(EventEffectiveProfileSwitchChanged(eps))
                rxBus.send(EventNewHistoryData(eps.timestamp, false))
            }
            it.filterIsInstance<TemporaryTarget>().firstOrNull()?.let { tt ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventTempTargetChange $tt")
                rxBus.send(EventTempTargetChange())
            }
            it.filterIsInstance<TherapyEvent>().firstOrNull()?.let { te ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventTherapyEventChange $te")
                rxBus.send(EventTherapyEventChange())
            }
            it.filterIsInstance<Food>().firstOrNull()?.let { food ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventFoodDatabaseChanged $food")
                rxBus.send(EventFoodDatabaseChanged())
            }
            it.filterIsInstance<ProfileSwitch>().firstOrNull()?.let { ps ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventProfileSwitchChanged $ps")
                rxBus.send(EventProfileSwitchChanged())
            }
            it.filterIsInstance<OfflineEvent>().firstOrNull()?.let { oe ->
                aapsLogger.debug(LTag.DATABASE, "Firing EventOfflineChange $oe")
                rxBus.send(EventOfflineChange())
            }
        }
}