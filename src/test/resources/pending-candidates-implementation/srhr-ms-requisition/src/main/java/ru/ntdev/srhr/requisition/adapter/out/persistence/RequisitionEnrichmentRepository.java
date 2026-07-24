package ru.ntdev.srhr.requisition.adapter.out.persistence;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.ntdev.srhr.pending.contracts.RequisitionView;
import ru.ntdev.srhr.pending.contracts.StaffPositionView;
import ru.ntdev.srhr.pending.contracts.StructUnitPathView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Единственная проектно-зависимая часть реализации. Названия таблиц/полей следует
 * сопоставить с существующей моделью srhr-ms-requisition. Алгоритм не делает N+1:
 * один запрос за заявками и один за путями подразделений.
 */
@Repository
public class RequisitionEnrichmentRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public RequisitionEnrichmentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, RequisitionView> findByVacancyIds(Collection<String> vacancyIds) {
        if (vacancyIds.isEmpty()) return Map.of();
        MapSqlParameterSource params = new MapSqlParameterSource("vacancyIds", vacancyIds);
        List<BaseRow> bases = jdbc.query("""
                select cast(v.id as varchar) as vacancy_id,
                       r.id as requisition_pk,
                       r.guid,
                       r.number,
                       r.position_type,
                       r.staff_position_id,
                       r.staff_position_name,
                       r.struct_unit_name,
                       r.struct_unit_id
                from vacancy v
                join requisition r on r.id = v.requisition_id
                where cast(v.id as varchar) in (:vacancyIds)
                """, params, (rs, rowNum) -> new BaseRow(
                rs.getString("vacancy_id"),
                rs.getLong("requisition_pk"),
                rs.getString("guid"),
                rs.getString("number"),
                rs.getString("position_type"),
                rs.getString("staff_position_id"),
                rs.getString("staff_position_name"),
                rs.getString("struct_unit_name"),
                rs.getString("struct_unit_id")
        ));
        if (bases.isEmpty()) return Map.of();

        List<Long> requisitionIds = bases.stream().map(BaseRow::requisitionPk).distinct().toList();
        Map<Long, List<StructUnitPathView>> paths = new HashMap<>();
        jdbc.query("""
                select requisition_id, struct_unit_id, type_desc, name
                from requisition_struct_unit_path
                where requisition_id in (:ids)
                order by requisition_id, path_order
                """, new MapSqlParameterSource("ids", requisitionIds), rs -> {
            long id = rs.getLong("requisition_id");
            paths.computeIfAbsent(id, ignored -> new ArrayList<>()).add(new StructUnitPathView(
                    rs.getString("struct_unit_id"),
                    rs.getString("type_desc"),
                    rs.getString("name")
            ));
        });

        Map<String, RequisitionView> result = new LinkedHashMap<>();
        for (BaseRow row : bases) {
            result.put(row.vacancyId(), new RequisitionView(
                    row.guid(), row.number(), row.positionType(),
                    new StaffPositionView(row.staffPositionId(), row.staffPositionName()),
                    row.structUnitName(), row.structUnitId(),
                    paths.getOrDefault(row.requisitionPk(), List.of())
            ));
        }
        return result;
    }

    private record BaseRow(
            String vacancyId,
            long requisitionPk,
            String guid,
            String number,
            String positionType,
            String staffPositionId,
            String staffPositionName,
            String structUnitName,
            String structUnitId
    ) {}
}
