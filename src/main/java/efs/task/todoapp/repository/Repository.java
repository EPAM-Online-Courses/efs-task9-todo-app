package efs.task.todoapp.repository;

import java.util.List;
import java.util.function.Predicate;

public interface Repository<ID, ENTITY> {

    /**
     * Stores the entity in the repository.
     *
     * @param entity entity to be stored
     * @return identifier of the entry, <code>null</code> if entry has not been saved
     */
    ID save(ENTITY entity);

    /**
     * Returns entity with specified identifier.
     *
     * @param id identifier whose associated entity is to be returned
     * @return entity with specified identifier or <code>null</code> if the repository does not contain such entity
     */
    ENTITY query(ID id);

    /**
     * Returns entities satisfying specified condition from the repository.
     *
     * @param condition condition needs to be satisfied
     * @return list of entities satisfying specified predicate
     */
    List<ENTITY> query(Predicate<ENTITY> condition);

    /**
     * Updates entity with specified identifier.
     *
     * @param id identifier whose associated entity is to be updated
     * @param entity entity with new data
     * @return updated entity with specified identifier or <code>null</code> if the repository does not contain such entity
     */
    ENTITY update(ID id, ENTITY entity);

    /**
     * Deletes entity with specified identifier.
     *
     * @param id identifier whose associated entity is to be updated
     * @return <code>true</code> if the repository contained entity with the specified identifier
     */
    boolean delete(ID id);
}
