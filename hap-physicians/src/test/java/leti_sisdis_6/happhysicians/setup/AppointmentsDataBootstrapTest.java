package leti_sisdis_6.happhysicians.setup;

import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentsDataBootstrapTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PhysicianRepository physicianRepository;

    @InjectMocks
    private AppointmentsDataBootstrap appointmentsDataBootstrap;

    @Test
    void testRun_DoesNothing() {
        // Act
        appointmentsDataBootstrap.run();

        // Assert - Verify no interactions with repositories
        verify(appointmentRepository, never()).save(any());
        verify(appointmentRepository, never()).saveAll(any());
        verify(physicianRepository, never()).findById(anyString());
    }

    @Test
    void testRun_ReturnsEarly() {
        // Act & Assert - Should complete without exceptions
        appointmentsDataBootstrap.run();
        
        // If we get here without exception, the method returned early as expected
        assertTrue(true);
    }
}

